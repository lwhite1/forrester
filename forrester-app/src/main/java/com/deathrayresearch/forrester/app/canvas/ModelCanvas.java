package com.deathrayresearch.forrester.app.canvas;

import com.deathrayresearch.forrester.model.def.AuxDef;
import com.deathrayresearch.forrester.model.def.ConnectorRoute;
import com.deathrayresearch.forrester.model.def.ConstantDef;
import com.deathrayresearch.forrester.model.def.ElementType;
import com.deathrayresearch.forrester.model.def.FlowDef;
import com.deathrayresearch.forrester.model.def.ModelDefinition;
import com.deathrayresearch.forrester.model.def.ModuleInstanceDef;
import com.deathrayresearch.forrester.model.def.ViewDef;
import com.deathrayresearch.forrester.model.graph.AutoLayout;
import com.deathrayresearch.forrester.model.graph.DependencyGraph;
import com.deathrayresearch.forrester.model.graph.FeedbackAnalysis;

import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Canvas component that renders a model using the Layered Flow Diagram visual language.
 * Supports pan (Space+drag, middle/right drag), zoom (scroll wheel),
 * click-to-select, drag-to-move, element creation (toolbar placement mode),
 * two-click flow connection, inline name/value editing, and element deletion.
 */
public class ModelCanvas extends Canvas {

    private static final double ZOOM_FACTOR = 1.1;

    private ModelEditor editor;
    private List<ConnectorRoute> connectors = List.of();

    private final Viewport viewport = new Viewport();
    private final CanvasState canvasState = new CanvasState();
    private final CanvasRenderer renderer = new CanvasRenderer(canvasState, viewport);

    // Tool state
    private CanvasToolBar.Tool activeTool = CanvasToolBar.Tool.SELECT;
    private CanvasToolBar toolBar;

    // Drag/pan state
    private boolean dragging;
    private boolean panning;
    private boolean panMoved;
    private boolean spaceDown;
    private double dragStartX;
    private double dragStartY;
    private String dragTarget;
    private final Map<String, CanvasState.Position> dragStartPositions = new HashMap<>();

    // Mouse position tracking for cursor updates
    private double lastMouseX;
    private double lastMouseY;

    // Rubber-band (marquee) selection
    private boolean marqueeActive;
    private double marqueeStartWorldX;
    private double marqueeStartWorldY;
    private double marqueeEndWorldX;
    private double marqueeEndWorldY;
    private Set<String> marqueeInitialSelection;

    // Two-click flow creation
    private final FlowCreationController flowCreation = new FlowCreationController();

    // Flow endpoint reattachment drag
    private boolean reattaching;
    private String reattachFlowName;
    private FlowEndpointCalculator.FlowEnd reattachEnd;
    private double reattachDiamondX;
    private double reattachDiamondY;
    private double reattachRubberBandX;
    private double reattachRubberBandY;

    // Inline editor
    private Pane overlayPane;
    private InlineEditor inlineEditor;

    // Undo/redo
    private UndoManager undoManager;
    private boolean dragUndoSaved;

    // Resize state
    private boolean resizing;
    private String resizeTarget;
    private ResizeHandle resizeHandle;
    private double resizeAnchorX;
    private double resizeAnchorY;
    private double resizeStartWidth;
    private double resizeStartHeight;
    private double resizeStartCenterX;
    private double resizeStartCenterY;
    private boolean resizeUndoSaved;

    // Status change callback
    private Runnable onStatusChanged;

    // Module navigation
    private final NavigationStack navigationStack = new NavigationStack();
    private Runnable onNavigationChanged;
    private ContextMenu contextMenu;

    // Hover highlighting
    private String hoveredElement;
    private ConnectionId hoveredConnection;
    private ConnectionId selectedConnection;

    // Feedback loop highlighting
    private boolean loopHighlightActive;
    private FeedbackAnalysis loopAnalysis;

    public ModelCanvas() {
        setFocusTraversable(true);

        widthProperty().addListener(observable -> redraw());
        heightProperty().addListener(observable -> redraw());

        setOnScroll(this::handleScroll);
        setOnMousePressed(this::handleMousePressed);
        setOnMouseDragged(this::handleMouseDragged);
        setOnMouseReleased(this::handleMouseReleased);
        setOnMouseMoved(this::handleMouseMoved);
        setOnKeyPressed(this::handleKeyPressed);
        setOnKeyReleased(this::handleKeyReleased);
    }

    /**
     * Sets the model editor and loads canvas state from a layout view, triggering a redraw.
     */
    public void setModel(ModelEditor editor, ViewDef view) {
        this.editor = editor;
        canvasState.loadFrom(view);
        this.connectors = editor.generateConnectors();
        invalidateLoopAnalysis();
        redraw();
    }

    /**
     * Returns the current canvas layout as a {@link ViewDef} for serialization.
     */
    public ViewDef toViewDef() {
        return canvasState.toViewDef();
    }

    /**
     * Returns the model editor.
     */
    public ModelEditor getEditor() {
        return editor;
    }

    /**
     * Builds an immutable {@link ModelDefinition} snapshot including the current canvas layout.
     * When inside a module, walks the navigation stack to reconstruct the full root definition.
     */
    public ModelDefinition toModelDefinition() {
        if (navigationStack.isEmpty()) {
            return editor.toModelDefinition(canvasState.toViewDef());
        }

        // Build current level's definition with its view
        ModelDefinition childDef = editor.toModelDefinition(canvasState.toViewDef());

        // Walk the stack from top (most recent parent) to bottom (root),
        // writing each child into its parent
        List<NavigationStack.Frame> frames = new ArrayList<>(navigationStack.frames());
        // frames() returns bottom-to-top; we need to process top-to-bottom
        for (int i = frames.size() - 1; i >= 0; i--) {
            NavigationStack.Frame frame = frames.get(i);
            ModelEditor parentEditor = new ModelEditor();
            parentEditor.loadFrom(frame.editor().toModelDefinition(frame.viewSnapshot()));
            parentEditor.updateModuleDefinition(frame.moduleIndex(), childDef);
            childDef = parentEditor.toModelDefinition(frame.viewSnapshot());
        }

        return childDef;
    }

    /**
     * Sets a callback to be invoked whenever the canvas status changes
     * (selection, tool, zoom, or element count changes).
     */
    public void setOnStatusChanged(Runnable callback) {
        this.onStatusChanged = callback;
    }

    private void fireStatusChanged() {
        if (onStatusChanged != null) {
            onStatusChanged.run();
        }
    }

    /**
     * Toggles feedback loop highlighting. When activated, computes a
     * {@link FeedbackAnalysis} from the current model and stores the result.
     * When deactivated, clears the cached analysis.
     */
    public void setLoopHighlightActive(boolean active) {
        this.loopHighlightActive = active;
        if (active && editor != null) {
            recomputeLoopAnalysis();
        } else {
            this.loopAnalysis = null;
        }
        redraw();
    }

    public boolean isLoopHighlightActive() {
        return loopHighlightActive;
    }

    /**
     * Returns the current loop analysis, or null if loop highlighting is not active.
     */
    public FeedbackAnalysis getLoopAnalysis() {
        return loopAnalysis;
    }

    private void recomputeLoopAnalysis() {
        if (editor == null) {
            loopAnalysis = null;
            return;
        }
        DependencyGraph graph = DependencyGraph.fromDefinition(
                editor.toModelDefinition(canvasState.toViewDef()));
        loopAnalysis = FeedbackAnalysis.analyze(graph);
    }

    /**
     * Invalidates the cached loop analysis so it is recomputed on the next redraw
     * (if loop highlighting is active).
     */
    private void invalidateLoopAnalysis() {
        if (loopHighlightActive && editor != null) {
            recomputeLoopAnalysis();
        }
    }

    /**
     * Sets the undo manager used for undo/redo operations.
     */
    public void setUndoManager(UndoManager undoManager) {
        this.undoManager = undoManager;
    }

    /**
     * Returns the undo manager, or null if not set.
     */
    public UndoManager getUndoManager() {
        return undoManager;
    }

    /**
     * Captures the current model and view state as an immutable snapshot.
     */
    private UndoManager.Snapshot captureSnapshot() {
        return new UndoManager.Snapshot(
                editor.toModelDefinition(canvasState.toViewDef()),
                canvasState.toViewDef());
    }

    /**
     * Saves the current state to the undo stack before a mutation.
     */
    private void saveUndoState() {
        if (undoManager != null && editor != null) {
            undoManager.pushUndo(captureSnapshot());
        }
    }

    /**
     * Restores a snapshot by reloading both the model editor and canvas state.
     */
    private void restoreSnapshot(UndoManager.Snapshot snapshot) {
        editor.loadFrom(snapshot.model());
        canvasState.loadFrom(snapshot.view());
        connectors = editor.generateConnectors();
        invalidateLoopAnalysis();
        redraw();
    }

    /**
     * Undoes the last operation, restoring the previous state.
     */
    public void performUndo() {
        if (undoManager == null || editor == null) {
            return;
        }
        UndoManager.Snapshot previous = undoManager.undo(captureSnapshot());
        if (previous != null) {
            restoreSnapshot(previous);
        }
    }

    /**
     * Redoes the last undone operation, restoring the next state.
     */
    public void performRedo() {
        if (undoManager == null || editor == null) {
            return;
        }
        UndoManager.Snapshot next = undoManager.redo(captureSnapshot());
        if (next != null) {
            restoreSnapshot(next);
        }
    }

    /**
     * Returns the current selection count.
     */
    public int getSelectionCount() {
        return canvasState.getSelection().size();
    }

    /**
     * Returns the names of all currently selected elements.
     */
    public Set<String> getSelectedElementNames() {
        return canvasState.getSelection();
    }

    /**
     * Returns the currently selected connection, or null if no connection is selected.
     */
    public ConnectionId getSelectedConnection() {
        return selectedConnection;
    }

    /**
     * Returns the type of the named element on the canvas.
     */
    public ElementType getSelectedElementType(String name) {
        return canvasState.getType(name);
    }

    /**
     * Public wrapper for deleting all currently selected elements.
     */
    public void deleteSelectedElements() {
        deleteSelected();
    }

    /**
     * Renames an element with undo support.
     */
    public void renameElement(String oldName, String newName) {
        applyRename(oldName, newName);
    }

    /**
     * Opens the bindings configuration dialog for the named module (public accessor).
     */
    public void triggerBindingConfig(String moduleName) {
        openBindingsDialog(moduleName);
    }

    /**
     * Saves the current state to the undo stack (public accessor for PropertiesPanel).
     */
    public void pushUndoState() {
        saveUndoState();
    }

    /**
     * Regenerates connectors from the current model state and redraws.
     * Called by the properties panel after editing equations or other
     * changes that affect dependency relationships.
     */
    public void regenerateAndRedraw() {
        connectors = editor.generateConnectors();
        invalidateLoopAnalysis();
        redraw();
    }

    /**
     * Returns the canvas state for export or external rendering.
     */
    public CanvasState getCanvasState() {
        return canvasState;
    }

    /**
     * Returns the current connector routes.
     */
    public List<ConnectorRoute> getConnectors() {
        return connectors;
    }

    /**
     * Returns the active loop analysis if loop highlighting is on, otherwise null.
     */
    public FeedbackAnalysis getActiveLoopAnalysis() {
        return loopHighlightActive ? loopAnalysis : null;
    }

    /**
     * Returns the current zoom scale.
     */
    public double getZoomScale() {
        return viewport.getScale();
    }

    /**
     * Sets the active tool (called by toolbar callback).
     * Cancels any pending flow if the tool changes.
     */
    public void setActiveTool(CanvasToolBar.Tool tool) {
        if (flowCreation.isPending()) {
            flowCreation.cancel();
        }
        this.activeTool = tool;
        updateCursor();
    }

    /**
     * Sets a reference to the toolbar so the canvas can reset it on Escape.
     */
    public void setToolBar(CanvasToolBar toolBar) {
        this.toolBar = toolBar;
    }

    /**
     * Sets the parent pane for inline editor overlays and creates the InlineEditor.
     * Must be called after the canvas is added to the pane.
     */
    public void setOverlayPane(Pane overlayPane) {
        this.overlayPane = overlayPane;
        this.inlineEditor = new InlineEditor(overlayPane);
    }

    @Override
    public boolean isResizable() {
        return true;
    }

    @Override
    public double prefWidth(double height) {
        return getWidth();
    }

    @Override
    public double prefHeight(double width) {
        return getHeight();
    }

    /**
     * Redraws the entire canvas by delegating to the CanvasRenderer,
     * then notifies the status bar of any changes.
     */
    private void redraw() {
        CanvasRenderer.ReattachState reattachState = reattaching
                ? new CanvasRenderer.ReattachState(true, reattachDiamondX, reattachDiamondY,
                        reattachRubberBandX, reattachRubberBandY)
                : CanvasRenderer.ReattachState.IDLE;
        CanvasRenderer.MarqueeState marqueeState = marqueeActive
                ? new CanvasRenderer.MarqueeState(true, marqueeStartWorldX, marqueeStartWorldY,
                        marqueeEndWorldX, marqueeEndWorldY)
                : CanvasRenderer.MarqueeState.IDLE;
        renderer.render(getGraphicsContext2D(), getWidth(), getHeight(),
                editor, connectors, flowCreation.getState(), reattachState, marqueeState,
                loopHighlightActive ? loopAnalysis : null, hoveredElement,
                hoveredConnection, selectedConnection);
        fireStatusChanged();
    }

    /**
     * Creates a new element at the given world coordinates based on the active tool.
     * Adds to both the model editor and canvas state, then regenerates connectors.
     * PLACE_FLOW is handled separately by {@link #handleFlowClick}.
     */
    private void createElementAt(double worldX, double worldY) {
        if (editor == null) {
            return;
        }

        saveUndoState();

        String name;
        ElementType type;

        switch (activeTool) {
            case PLACE_STOCK -> {
                name = editor.addStock();
                type = ElementType.STOCK;
            }
            case PLACE_AUX -> {
                name = editor.addAux();
                type = ElementType.AUX;
            }
            case PLACE_CONSTANT -> {
                name = editor.addConstant();
                type = ElementType.CONSTANT;
            }
            case PLACE_MODULE -> {
                name = editor.addModule();
                type = ElementType.MODULE;
            }
            default -> {
                return;
            }
        }

        canvasState.addElement(name, type, worldX, worldY);
        connectors = editor.generateConnectors();
        invalidateLoopAnalysis();
        canvasState.clearSelection();
        canvasState.select(name);
        redraw();
    }

    /**
     * Deletes all currently selected elements from the model and canvas.
     * Regenerates connectors after removal.
     */
    private void deleteSelected() {
        if (editor == null || canvasState.getSelection().isEmpty()) {
            return;
        }

        saveUndoState();

        List<String> toDelete = new ArrayList<>(canvasState.getSelection());
        for (String name : toDelete) {
            editor.removeElement(name);
            canvasState.removeElement(name);
        }

        connectors = editor.generateConnectors();
        invalidateLoopAnalysis();
        redraw();
        updateCursor();
    }

    // --- Two-click flow creation ---

    /**
     * Handles a click during PLACE_FLOW mode by delegating to the FlowCreationController.
     */
    private void handleFlowClick(double worldX, double worldY) {
        if (flowCreation.isPending()) {
            saveUndoState();
        }
        FlowCreationController.FlowResult result = flowCreation.handleClick(
                worldX, worldY, canvasState, editor);
        if (result.isCreated()) {
            connectors = editor.generateConnectors();
            invalidateLoopAnalysis();
            canvasState.clearSelection();
            canvasState.select(result.flowName());
        }
        redraw();
    }

    // --- Inline editing ---

    /**
     * Starts inline editing for the given element on double-click.
     */
    private void startInlineEdit(String elementName) {
        if (inlineEditor == null || inlineEditor.isActive()) {
            return;
        }

        ElementType type = canvasState.getType(elementName);
        if (type == null) {
            return;
        }

        double worldX = canvasState.getX(elementName);
        double worldY = canvasState.getY(elementName);
        double screenX = viewport.toScreenX(worldX);
        double screenY = viewport.toScreenY(worldY);
        double fieldWidth = (LayoutMetrics.widthFor(type) + 20) * viewport.getScale();

        switch (type) {
            case CONSTANT -> startNameEditThenChain(elementName, screenX, screenY, fieldWidth,
                    name -> startConstantValueEdit(name, screenX, screenY, fieldWidth));
            case FLOW -> startNameEditThenChain(elementName, screenX, screenY, fieldWidth,
                    name -> startFlowEquationEdit(name, screenX, screenY));
            case AUX -> startNameEditThenChain(elementName, screenX, screenY, fieldWidth,
                    name -> startAuxEquationEdit(name, screenX, screenY));
            default -> inlineEditor.open(screenX, screenY, elementName, fieldWidth, newName -> {
                if (newName != null && !newName.equals(elementName)
                        && ModelEditor.isValidName(newName)) {
                    applyRename(elementName, newName);
                }
                requestFocus();
                updateCursor();
            });
        }
    }

    /**
     * Starts a name edit, then chains to a follow-up action (value or equation editing).
     * Common logic for constants, flows, and auxiliaries.
     */
    private void startNameEditThenChain(String elementName, double screenX, double screenY,
                                        double fieldWidth, Consumer<String> chainAction) {
        inlineEditor.open(screenX, screenY, elementName, fieldWidth, newName -> {
            String effectiveName;
            if (newName != null && !newName.equals(elementName)
                    && ModelEditor.isValidName(newName)) {
                applyRename(elementName, newName);
                effectiveName = newName;
            } else {
                effectiveName = elementName;
            }
            chainAction.accept(effectiveName);
        });
    }

    /**
     * Starts editing a constant's value after the name edit completes.
     */
    private void startConstantValueEdit(String constantName, double screenX, double screenY,
                                        double fieldWidth) {
        ConstantDef cd = findConstant(constantName);
        String currentValue = cd != null ? ElementRenderer.formatValue(cd.value()) : "0";

        double valueScreenY = screenY + 16 * viewport.getScale();

        inlineEditor.open(screenX, valueScreenY, currentValue, fieldWidth, valueText -> {
            if (valueText != null && !valueText.isBlank()) {
                try {
                    double value = Double.parseDouble(valueText);
                    saveUndoState();
                    editor.setConstantValue(constantName, value);
                    redraw();
                } catch (NumberFormatException ignored) {
                    // Invalid number — ignore
                }
            }
            requestFocus();
            updateCursor();
        });
    }

    /**
     * Starts editing a flow's equation after the name edit completes.
     * Uses wider field and positions below the diamond + name label.
     */
    private void startFlowEquationEdit(String flowName, double screenX, double screenY) {
        FlowDef fd = findFlow(flowName);
        String currentEquation = fd != null ? fd.equation() : "0";

        double eqScreenY = screenY + LayoutMetrics.FLOW_EQUATION_EDITOR_OFFSET * viewport.getScale();
        double eqFieldWidth = Math.max(LayoutMetrics.EQUATION_EDITOR_MIN_WIDTH,
                LayoutMetrics.AUX_WIDTH + 20) * viewport.getScale();

        inlineEditor.open(screenX, eqScreenY, currentEquation, eqFieldWidth, eqText -> {
            if (eqText != null && !eqText.isBlank()) {
                saveUndoState();
                editor.setFlowEquation(flowName, eqText);
                connectors = editor.generateConnectors();
                invalidateLoopAnalysis();
                redraw();
            }
            requestFocus();
            updateCursor();
        });
    }

    /**
     * Starts editing an auxiliary's equation after the name edit completes.
     * Uses wider field and positions below the name inside the rectangle.
     */
    private void startAuxEquationEdit(String auxName, double screenX, double screenY) {
        AuxDef ad = findAux(auxName);
        String currentEquation = ad != null ? ad.equation() : "0";

        double eqScreenY = screenY + LayoutMetrics.LABEL_SUBLABEL_OFFSET * viewport.getScale();
        double eqFieldWidth = Math.max(LayoutMetrics.EQUATION_EDITOR_MIN_WIDTH,
                LayoutMetrics.AUX_WIDTH + 20) * viewport.getScale();

        inlineEditor.open(screenX, eqScreenY, currentEquation, eqFieldWidth, eqText -> {
            if (eqText != null && !eqText.isBlank()) {
                saveUndoState();
                editor.setAuxEquation(auxName, eqText);
                connectors = editor.generateConnectors();
                invalidateLoopAnalysis();
                redraw();
            }
            requestFocus();
            updateCursor();
        });
    }

    /**
     * Applies a rename to both the model editor and canvas state,
     * then regenerates connectors and redraws.
     */
    private void applyRename(String oldName, String newName) {
        if (oldName.equals(newName) || editor.hasElement(newName)) {
            return;
        }
        saveUndoState();
        if (!editor.renameElement(oldName, newName)) {
            return;
        }
        canvasState.renameElement(oldName, newName);
        connectors = editor.generateConnectors();
        invalidateLoopAnalysis();
        redraw();
    }

    private ConstantDef findConstant(String name) {
        for (ConstantDef c : editor.getConstants()) {
            if (c.name().equals(name)) {
                return c;
            }
        }
        return null;
    }

    private FlowDef findFlow(String name) {
        for (FlowDef f : editor.getFlows()) {
            if (f.name().equals(name)) {
                return f;
            }
        }
        return null;
    }

    private AuxDef findAux(String name) {
        for (AuxDef a : editor.getAuxiliaries()) {
            if (a.name().equals(name)) {
                return a;
            }
        }
        return null;
    }

    // --- Flow endpoint reattachment ---

    /**
     * Begins a reattachment drag from a cloud or connected endpoint hit.
     */
    private void startReattachment(FlowEndpointCalculator.CloudHit hit) {
        reattaching = true;
        reattachFlowName = hit.flowName();
        reattachEnd = hit.end();
        // Diamond center of the flow
        reattachDiamondX = canvasState.getX(hit.flowName());
        reattachDiamondY = canvasState.getY(hit.flowName());
        reattachRubberBandX = hit.cloudX();
        reattachRubberBandY = hit.cloudY();
        redraw();
    }

    /**
     * Completes a reattachment drag: if released on a stock, reconnects the flow
     * endpoint to that stock; if released on empty space, disconnects to cloud.
     */
    private void completeReattachment(double worldX, double worldY) {
        saveUndoState();
        String stockHit = FlowCreationController.hitTestStockOnly(worldX, worldY, canvasState);
        editor.reconnectFlow(reattachFlowName, reattachEnd, stockHit);
        connectors = editor.generateConnectors();
        invalidateLoopAnalysis();
        cancelReattachment();
        redraw();
    }

    /**
     * Cancels a reattachment drag without making any changes.
     */
    private void cancelReattachment() {
        reattaching = false;
        reattachFlowName = null;
        reattachEnd = null;
    }

    // --- Marquee selection ---

    /**
     * Updates the selection based on elements inside the marquee rectangle.
     * Restores initial selection first (for Shift+marquee behavior).
     */
    private void updateMarqueeSelection() {
        double minX = Math.min(marqueeStartWorldX, marqueeEndWorldX);
        double maxX = Math.max(marqueeStartWorldX, marqueeEndWorldX);
        double minY = Math.min(marqueeStartWorldY, marqueeEndWorldY);
        double maxY = Math.max(marqueeStartWorldY, marqueeEndWorldY);

        canvasState.clearSelection();
        if (marqueeInitialSelection != null) {
            for (String name : marqueeInitialSelection) {
                canvasState.addToSelection(name);
            }
        }
        for (String name : canvasState.getDrawOrder()) {
            double cx = canvasState.getX(name);
            double cy = canvasState.getY(name);
            if (cx >= minX && cx <= maxX && cy >= minY && cy <= maxY) {
                canvasState.addToSelection(name);
            }
        }
    }

    /**
     * Cancels the marquee selection, restoring the selection to its pre-marquee state.
     */
    private void cancelMarquee() {
        canvasState.clearSelection();
        if (marqueeInitialSelection != null) {
            for (String name : marqueeInitialSelection) {
                canvasState.addToSelection(name);
            }
        }
        marqueeActive = false;
        marqueeInitialSelection = null;
    }

    // --- Resize ---

    private void startResize(ResizeHandle.HandleHit hit, double worldX, double worldY) {
        resizing = true;
        resizeTarget = hit.elementName();
        resizeHandle = hit.handle();
        resizeUndoSaved = false;

        double cx = canvasState.getX(resizeTarget);
        double cy = canvasState.getY(resizeTarget);
        resizeStartCenterX = cx;
        resizeStartCenterY = cy;
        resizeStartWidth = LayoutMetrics.effectiveWidth(canvasState, resizeTarget);
        resizeStartHeight = LayoutMetrics.effectiveHeight(canvasState, resizeTarget);

        double halfW = resizeStartWidth / 2 + SelectionRenderer.SELECTION_PADDING;
        double halfH = resizeStartHeight / 2 + SelectionRenderer.SELECTION_PADDING;

        // Anchor is the corner opposite the grabbed handle
        switch (resizeHandle) {
            case TOP_LEFT -> { resizeAnchorX = cx + halfW; resizeAnchorY = cy + halfH; }
            case TOP_RIGHT -> { resizeAnchorX = cx - halfW; resizeAnchorY = cy + halfH; }
            case BOTTOM_LEFT -> { resizeAnchorX = cx + halfW; resizeAnchorY = cy - halfH; }
            case BOTTOM_RIGHT -> { resizeAnchorX = cx - halfW; resizeAnchorY = cy - halfH; }
        }
    }

    private void handleResizeDrag(double worldX, double worldY) {
        if (!resizeUndoSaved) {
            saveUndoState();
            resizeUndoSaved = true;
        }

        ElementType type = canvasState.getType(resizeTarget);
        double minW = LayoutMetrics.minWidthFor(type);
        double minH = LayoutMetrics.minHeightFor(type);
        double pad = SelectionRenderer.SELECTION_PADDING;

        double rawW = Math.abs(worldX - resizeAnchorX) - pad;
        double rawH = Math.abs(worldY - resizeAnchorY) - pad;
        double newW = Math.max(minW, rawW);
        double newH = Math.max(minH, rawH);

        // Compute new center as midpoint between anchor and effective drag edge
        double edgeX = resizeAnchorX + Math.signum(worldX - resizeAnchorX) * (newW / 2 + pad);
        double edgeY = resizeAnchorY + Math.signum(worldY - resizeAnchorY) * (newH / 2 + pad);
        double newCx = (resizeAnchorX + edgeX) / 2;
        double newCy = (resizeAnchorY + edgeY) / 2;

        canvasState.setSize(resizeTarget, newW, newH);
        canvasState.setPosition(resizeTarget, newCx, newCy);
    }

    private void cancelResize() {
        // If we saved an undo state, revert to it
        if (resizeUndoSaved) {
            performUndo();
        }

        resizing = false;
        resizeTarget = null;
        resizeHandle = null;
    }

    private static Cursor resizeCursorFor(ResizeHandle handle) {
        return switch (handle) {
            case TOP_LEFT, BOTTOM_RIGHT -> Cursor.NW_RESIZE;
            case TOP_RIGHT, BOTTOM_LEFT -> Cursor.NE_RESIZE;
        };
    }

    // --- Cursor ---

    /**
     * Updates the cursor shape based on the current interaction state.
     */
    private void updateCursor() {
        if (inlineEditor != null && inlineEditor.isActive()) {
            return;
        }

        Cursor cursor;

        if (resizing) {
            cursor = resizeCursorFor(resizeHandle);
        } else if (reattaching || panning || dragging) {
            cursor = Cursor.CLOSED_HAND;
        } else if (marqueeActive) {
            cursor = Cursor.CROSSHAIR;
        } else if (spaceDown) {
            cursor = Cursor.MOVE;
        } else if (flowCreation.isPending() || activeTool != CanvasToolBar.Tool.SELECT) {
            cursor = Cursor.CROSSHAIR;
        } else if (editor != null) {
            double worldX = viewport.toWorldX(lastMouseX);
            double worldY = viewport.toWorldY(lastMouseY);

            // Check resize handles first (when hovering over selected elements)
            ResizeHandle.HandleHit handleHit = ResizeHandle.hitTest(canvasState, worldX, worldY);
            if (handleHit != null) {
                cursor = resizeCursorFor(handleHit.handle());
            } else {
                FlowEndpointCalculator.CloudHit cloudHit =
                        FlowEndpointCalculator.hitTestClouds(worldX, worldY, canvasState, editor);
                if (cloudHit == null) {
                    cloudHit = FlowEndpointCalculator.hitTestConnectedEndpoints(
                            worldX, worldY, canvasState, editor);
                }

                if (cloudHit != null) {
                    cursor = Cursor.HAND;
                } else {
                    String hit = HitTester.hitTest(canvasState, worldX, worldY);
                    if (hit != null) {
                        cursor = Cursor.OPEN_HAND;
                    } else if (hoveredConnection != null) {
                        cursor = Cursor.HAND;
                    } else {
                        cursor = Cursor.DEFAULT;
                    }
                }
            }
        } else {
            cursor = Cursor.DEFAULT;
        }

        setCursor(cursor);
    }

    // --- Event handlers ---

    private void handleScroll(ScrollEvent event) {
        double factor = event.getDeltaY() > 0 ? ZOOM_FACTOR : 1.0 / ZOOM_FACTOR;
        viewport.zoomAt(event.getX(), event.getY(), factor);
        redraw();
        updateCursor();
        event.consume();
    }

    private void handleMouseMoved(MouseEvent event) {
        lastMouseX = event.getX();
        lastMouseY = event.getY();

        if (flowCreation.isPending()) {
            flowCreation.updateRubberBand(
                    viewport.toWorldX(event.getX()),
                    viewport.toWorldY(event.getY()));
            redraw();
            event.consume();
        }

        // Update hover highlight
        double worldX = viewport.toWorldX(event.getX());
        double worldY = viewport.toWorldY(event.getY());
        String hit = HitTester.hitTest(canvasState, worldX, worldY);

        // Connection hover: only when no element is hovered
        ConnectionId connHit = null;
        if (hit == null) {
            connHit = HitTester.hitTestInfoLink(canvasState, connectors, worldX, worldY);
        }

        boolean changed = !Objects.equals(hit, hoveredElement)
                || !Objects.equals(connHit, hoveredConnection);
        if (changed) {
            hoveredElement = hit;
            hoveredConnection = connHit;
            redraw();
        }

        updateCursor();
    }

    private void handleMousePressed(MouseEvent event) {
        // Guard: ignore mouse clicks while inline editor is active
        if (inlineEditor != null && inlineEditor.isActive()) {
            return;
        }

        requestFocus();
        lastMouseX = event.getX();
        lastMouseY = event.getY();
        dragStartX = event.getX();
        dragStartY = event.getY();

        // Pan: middle-drag, right-drag, or Space+left-drag
        if (event.getButton() == MouseButton.MIDDLE
                || event.getButton() == MouseButton.SECONDARY
                || (event.getButton() == MouseButton.PRIMARY && spaceDown)) {
            panning = true;
            panMoved = false;
            updateCursor();
            event.consume();
            return;
        }

        if (event.getButton() == MouseButton.PRIMARY) {
            double worldX = viewport.toWorldX(event.getX());
            double worldY = viewport.toWorldY(event.getY());

            // Double-click: drill into module or start inline editing
            if (event.getClickCount() == 2
                    && activeTool == CanvasToolBar.Tool.SELECT
                    && !flowCreation.isPending()) {
                String hit = HitTester.hitTest(canvasState, worldX, worldY);
                if (hit != null) {
                    ElementType hitType = canvasState.getType(hit);
                    if (hitType == ElementType.MODULE) {
                        drillInto(hit);
                    } else {
                        startInlineEdit(hit);
                    }
                    event.consume();
                    return;
                }
            }

            // Flow endpoint reattachment: check cloud + connected endpoints (SELECT mode only)
            if (activeTool == CanvasToolBar.Tool.SELECT && !flowCreation.isPending()) {
                FlowEndpointCalculator.CloudHit cloudHit =
                        FlowEndpointCalculator.hitTestClouds(worldX, worldY, canvasState, editor);
                if (cloudHit == null) {
                    cloudHit = FlowEndpointCalculator.hitTestConnectedEndpoints(
                            worldX, worldY, canvasState, editor);
                }
                if (cloudHit != null) {
                    startReattachment(cloudHit);
                    updateCursor();
                    event.consume();
                    return;
                }
            }

            // Resize handle check: takes priority over move drag
            if (activeTool == CanvasToolBar.Tool.SELECT && !flowCreation.isPending()) {
                ResizeHandle.HandleHit handleHit = ResizeHandle.hitTest(canvasState, worldX, worldY);
                if (handleHit != null) {
                    startResize(handleHit, worldX, worldY);
                    updateCursor();
                    event.consume();
                    return;
                }
            }

            // PLACE_FLOW: two-click protocol
            if (activeTool == CanvasToolBar.Tool.PLACE_FLOW) {
                handleFlowClick(worldX, worldY);
                updateCursor();
                event.consume();
                return;
            }

            // Placement mode: other PLACE_* tools — click on empty space to create
            if (activeTool != CanvasToolBar.Tool.SELECT) {
                String hit = HitTester.hitTest(canvasState, worldX, worldY);
                if (hit == null) {
                    createElementAt(worldX, worldY);
                    updateCursor();
                    event.consume();
                    return;
                }
            }

            // Select mode
            String hit = HitTester.hitTest(canvasState, worldX, worldY);

            if (hit != null) {
                // Element click: clear connection selection
                selectedConnection = null;

                if (event.isShiftDown()) {
                    canvasState.toggleSelection(hit);
                } else if (!canvasState.isSelected(hit)) {
                    canvasState.select(hit);
                }

                // Capture drag start positions for all selected elements
                dragTarget = hit;
                dragging = true;
                dragUndoSaved = false;
                dragStartPositions.clear();
                for (String name : canvasState.getSelection()) {
                    dragStartPositions.put(name,
                            new CanvasState.Position(canvasState.getX(name), canvasState.getY(name)));
                }
            } else {
                // No element hit — check for connection click
                ConnectionId connHit = HitTester.hitTestInfoLink(
                        canvasState, connectors, worldX, worldY);
                if (connHit != null) {
                    // Connection click: select connection, clear element selection
                    selectedConnection = connHit;
                    canvasState.clearSelection();
                    dragTarget = null;
                    dragging = false;
                } else {
                    // Empty space: clear connection selection, start marquee
                    selectedConnection = null;
                    marqueeActive = true;
                    marqueeStartWorldX = worldX;
                    marqueeStartWorldY = worldY;
                    marqueeEndWorldX = worldX;
                    marqueeEndWorldY = worldY;
                    marqueeInitialSelection = new LinkedHashSet<>(canvasState.getSelection());
                    if (!event.isShiftDown()) {
                        canvasState.clearSelection();
                    }
                    dragTarget = null;
                    dragging = false;
                }
            }

            redraw();
            updateCursor();
            event.consume();
        }
    }

    private void handleMouseDragged(MouseEvent event) {
        hoveredElement = null;
        hoveredConnection = null;

        if (marqueeActive) {
            marqueeEndWorldX = viewport.toWorldX(event.getX());
            marqueeEndWorldY = viewport.toWorldY(event.getY());
            updateMarqueeSelection();
            redraw();
            event.consume();
            return;
        }

        if (reattaching) {
            reattachRubberBandX = viewport.toWorldX(event.getX());
            reattachRubberBandY = viewport.toWorldY(event.getY());
            redraw();
            event.consume();
            return;
        }

        if (resizing) {
            handleResizeDrag(viewport.toWorldX(event.getX()), viewport.toWorldY(event.getY()));
            redraw();
            event.consume();
            return;
        }

        double screenDx = event.getX() - dragStartX;
        double screenDy = event.getY() - dragStartY;

        if (panning) {
            panMoved = true;
            viewport.pan(screenDx, screenDy);
            dragStartX = event.getX();
            dragStartY = event.getY();
            redraw();
            event.consume();
            return;
        }

        if (dragging && dragTarget != null) {
            if (!dragUndoSaved) {
                saveUndoState();
                dragUndoSaved = true;
            }
            // Convert screen-space drag delta to world-space delta
            double worldDx = screenDx / viewport.getScale();
            double worldDy = screenDy / viewport.getScale();

            for (Map.Entry<String, CanvasState.Position> entry : dragStartPositions.entrySet()) {
                CanvasState.Position startPos = entry.getValue();
                canvasState.setPosition(entry.getKey(),
                        startPos.x() + worldDx,
                        startPos.y() + worldDy);
            }
            redraw();
            event.consume();
        }
    }

    private void handleMouseReleased(MouseEvent event) {
        if (marqueeActive) {
            marqueeActive = false;
            marqueeInitialSelection = null;
            redraw();
            updateCursor();
            event.consume();
            return;
        }

        if (reattaching) {
            completeReattachment(
                    viewport.toWorldX(event.getX()),
                    viewport.toWorldY(event.getY()));
            updateCursor();
            event.consume();
            return;
        }

        if (resizing) {
            resizing = false;
            resizeTarget = null;
            resizeHandle = null;
            updateCursor();
            event.consume();
            return;
        }

        // Right-click release without drag: show context menu on module
        if (panning && !panMoved && event.getButton() == MouseButton.SECONDARY) {
            double worldX = viewport.toWorldX(event.getX());
            double worldY = viewport.toWorldY(event.getY());
            String hit = HitTester.hitTest(canvasState, worldX, worldY);
            if (hit != null && canvasState.getType(hit) == ElementType.MODULE) {
                panning = false;
                panMoved = false;
                showElementContextMenu(hit, event.getScreenX(), event.getScreenY());
                updateCursor();
                event.consume();
                return;
            }
        }

        dragging = false;
        panning = false;
        panMoved = false;
        dragTarget = null;
        dragStartPositions.clear();
        updateCursor();
        event.consume();
    }

    /**
     * Selects all elements on the canvas.
     */
    public void selectAll() {
        canvasState.selectAll();
        redraw();
    }

    private void handleKeyPressed(KeyEvent event) {
        // Guard: ignore key events while inline editor is active
        if (inlineEditor != null && inlineEditor.isActive()) {
            return;
        }

        if (event.getCode() == KeyCode.ESCAPE) {
            handleEscape();
            event.consume();
        } else if (event.getCode() == KeyCode.SPACE) {
            spaceDown = true;
            updateCursor();
            event.consume();
        } else if (event.getCode() == KeyCode.DELETE || event.getCode() == KeyCode.BACK_SPACE) {
            deleteSelected();
            event.consume();
        } else if (event.isShortcutDown() && event.getCode() == KeyCode.A) {
            selectAll();
            event.consume();
        } else if (event.isShortcutDown()
                && (event.getCode() == KeyCode.PLUS || event.getCode() == KeyCode.EQUALS
                        || event.getCode() == KeyCode.ADD)) {
            viewport.zoomAt(getWidth() / 2, getHeight() / 2, ZOOM_FACTOR);
            redraw();
            updateCursor();
            event.consume();
        } else if (event.isShortcutDown()
                && (event.getCode() == KeyCode.MINUS || event.getCode() == KeyCode.SUBTRACT)) {
            viewport.zoomAt(getWidth() / 2, getHeight() / 2, 1.0 / ZOOM_FACTOR);
            redraw();
            updateCursor();
            event.consume();
        } else if (event.isShortcutDown() && event.getCode() == KeyCode.DIGIT0) {
            viewport.reset();
            redraw();
            updateCursor();
            event.consume();
        } else if (!event.isShortcutDown() && !event.isShiftDown() && !event.isAltDown()) {
            switch (event.getCode()) {
                case DIGIT1 -> { switchTool(CanvasToolBar.Tool.SELECT); event.consume(); }
                case DIGIT2 -> { switchTool(CanvasToolBar.Tool.PLACE_STOCK); event.consume(); }
                case DIGIT3 -> { switchTool(CanvasToolBar.Tool.PLACE_FLOW); event.consume(); }
                case DIGIT4 -> { switchTool(CanvasToolBar.Tool.PLACE_AUX); event.consume(); }
                case DIGIT5 -> { switchTool(CanvasToolBar.Tool.PLACE_CONSTANT); event.consume(); }
                case DIGIT6 -> { switchTool(CanvasToolBar.Tool.PLACE_MODULE); event.consume(); }
                default -> { }
            }
        }
    }

    /**
     * Handles Escape key with priority chain: cancel reattachment, cancel pending flow,
     * reset tool to Select, clear selection.
     */
    private void handleEscape() {
        if (resizing) {
            cancelResize();
            redraw();
        } else if (marqueeActive) {
            cancelMarquee();
            redraw();
        } else if (reattaching) {
            cancelReattachment();
            redraw();
        } else if (flowCreation.isPending()) {
            flowCreation.cancel();
            redraw();
        } else if (activeTool != CanvasToolBar.Tool.SELECT) {
            if (toolBar != null) {
                toolBar.resetToSelect();
            } else {
                activeTool = CanvasToolBar.Tool.SELECT;
            }
        } else if (selectedConnection != null) {
            selectedConnection = null;
            redraw();
        } else if (!canvasState.getSelection().isEmpty()) {
            canvasState.clearSelection();
            redraw();
        } else if (!navigationStack.isEmpty()) {
            navigateBack();
        }
        updateCursor();
    }

    private void switchTool(CanvasToolBar.Tool tool) {
        if (toolBar != null) {
            toolBar.selectTool(tool);
        } else {
            setActiveTool(tool);
        }
        fireStatusChanged();
    }

    private void handleKeyReleased(KeyEvent event) {
        if (event.getCode() == KeyCode.SPACE) {
            spaceDown = false;
            updateCursor();
            event.consume();
        }
    }

    // --- Module navigation ---

    /**
     * Sets a callback invoked whenever the navigation state changes
     * (drill in, navigate back, or clear).
     */
    public void setOnNavigationChanged(Runnable callback) {
        this.onNavigationChanged = callback;
    }

    private void fireNavigationChanged() {
        if (onNavigationChanged != null) {
            onNavigationChanged.run();
        }
    }

    /**
     * Drills into the named module, pushing the current level onto the navigation stack
     * and loading the module's inner definition for editing.
     */
    public void drillInto(String moduleName) {
        if (editor == null) {
            return;
        }
        ModuleInstanceDef module = editor.getModuleByName(moduleName);
        if (module == null) {
            return;
        }
        int moduleIndex = editor.getModuleIndex(moduleName);

        // Push current state onto navigation stack
        navigationStack.push(new NavigationStack.Frame(
                moduleName,
                moduleIndex,
                editor,
                canvasState.toViewDef(),
                viewport.getTranslateX(),
                viewport.getTranslateY(),
                viewport.getScale(),
                undoManager,
                activeTool
        ));

        // Create new editor loaded with module's inner definition
        ModelEditor moduleEditor = new ModelEditor();
        moduleEditor.loadFrom(module.definition());

        // Extract or auto-layout ViewDef from module's definition
        ViewDef moduleView;
        if (!module.definition().views().isEmpty()) {
            moduleView = module.definition().views().get(0);
        } else {
            moduleView = AutoLayout.layout(module.definition());
        }

        // Create a new UndoManager for this level
        UndoManager moduleUndoManager = new UndoManager();

        // Switch to the module's editor and view
        this.editor = moduleEditor;
        this.undoManager = moduleUndoManager;
        setModel(editor, moduleView);
        viewport.reset();

        // Reset tool to SELECT
        if (toolBar != null) {
            toolBar.resetToSelect();
        } else {
            activeTool = CanvasToolBar.Tool.SELECT;
        }

        fireNavigationChanged();
        fireStatusChanged();
    }

    /**
     * Navigates back to the parent level, writing the current level's definition
     * back into the parent's module.
     */
    public void navigateBack() {
        if (navigationStack.isEmpty()) {
            return;
        }

        // Capture current level as a ModelDefinition with its view
        ModelDefinition childDef = editor.toModelDefinition(canvasState.toViewDef());

        // Pop parent frame
        NavigationStack.Frame frame = navigationStack.pop();

        // Restore parent editor and save undo before write-back
        this.editor = frame.editor();
        this.undoManager = frame.undoManager();

        saveUndoState();
        editor.updateModuleDefinition(frame.moduleIndex(), childDef);

        // Restore parent canvas state and viewport
        canvasState.loadFrom(frame.viewSnapshot());
        viewport.restoreState(frame.viewportTranslateX(),
                frame.viewportTranslateY(), frame.viewportScale());

        // Restore active tool
        if (toolBar != null) {
            toolBar.selectTool(frame.activeTool());
        } else {
            activeTool = frame.activeTool();
        }

        // Regenerate connectors and redraw
        connectors = editor.generateConnectors();
        invalidateLoopAnalysis();
        redraw();

        fireNavigationChanged();
        fireStatusChanged();
    }

    /**
     * Navigates back to the given depth by repeatedly calling {@link #navigateBack()}.
     *
     * @param targetDepth the target depth (0 = root)
     */
    public void navigateToDepth(int targetDepth) {
        while (navigationStack.depth() > targetDepth) {
            navigateBack();
        }
    }

    /**
     * Returns true if currently editing inside a module (not at root level).
     */
    public boolean isInsideModule() {
        return !navigationStack.isEmpty();
    }

    /**
     * Returns the breadcrumb path from root to current level.
     */
    public List<String> getNavigationPath() {
        String rootName = navigationStack.isEmpty()
                ? editor.getModelName()
                : navigationStack.frames().get(0).editor().getModelName();
        return navigationStack.getPath(rootName);
    }

    /**
     * Returns the name of the current module being edited, or null if at root.
     */
    public String getCurrentModuleName() {
        if (navigationStack.isEmpty()) {
            return null;
        }
        // The most recent frame's moduleName is what we drilled into
        return navigationStack.peek().moduleName();
    }

    /**
     * Clears the navigation stack without writing back changes.
     * Used when opening a new file or creating a new model.
     */
    public void clearNavigation() {
        navigationStack.clear();
        fireNavigationChanged();
    }

    /**
     * Shows a context menu for the given element at the specified screen coordinates.
     */
    private void showElementContextMenu(String elementName, double screenX, double screenY) {
        if (contextMenu != null) {
            contextMenu.hide();
        }

        ElementType type = canvasState.getType(elementName);
        if (type != ElementType.MODULE) {
            return;
        }

        contextMenu = new ContextMenu();

        MenuItem drillItem = new MenuItem("Drill Into");
        drillItem.setOnAction(e -> drillInto(elementName));

        MenuItem bindingsItem = new MenuItem("Configure Bindings...");
        bindingsItem.setOnAction(e -> openBindingsDialog(elementName));

        MenuItem renameItem = new MenuItem("Rename");
        renameItem.setOnAction(e -> startInlineEdit(elementName));

        contextMenu.getItems().addAll(drillItem, bindingsItem,
                new SeparatorMenuItem(), renameItem);
        contextMenu.show(this, screenX, screenY);
    }

    /**
     * Opens the bindings configuration dialog for the named module.
     */
    private void openBindingsDialog(String moduleName) {
        ModuleInstanceDef module = editor.getModuleByName(moduleName);
        if (module == null) {
            return;
        }

        BindingConfigDialog dialog = new BindingConfigDialog(module);
        Optional<BindingConfigDialog.BindingResult> result = dialog.showAndWait();
        result.ifPresent(bindings -> {
            saveUndoState();
            editor.updateModuleBindings(moduleName,
                    bindings.inputBindings(), bindings.outputBindings());
            fireStatusChanged();
        });
    }

}

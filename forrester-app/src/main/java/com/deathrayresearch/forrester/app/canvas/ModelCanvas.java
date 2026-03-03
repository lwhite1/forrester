package com.deathrayresearch.forrester.app.canvas;

import com.deathrayresearch.forrester.model.def.ConnectorRoute;
import com.deathrayresearch.forrester.model.def.ElementType;
import com.deathrayresearch.forrester.model.def.LookupTableDef;
import com.deathrayresearch.forrester.model.def.ModelDefinition;
import com.deathrayresearch.forrester.model.def.ModuleInstanceDef;
import com.deathrayresearch.forrester.model.def.ViewDef;
import com.deathrayresearch.forrester.model.graph.AutoLayout;
import com.deathrayresearch.forrester.model.graph.DependencyGraph;
import com.deathrayresearch.forrester.model.graph.FeedbackAnalysis;

import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

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

    // Interaction controllers
    private final DragController dragController = new DragController();
    private final MarqueeController marqueeController = new MarqueeController();
    private final ResizeController resizeController = new ResizeController();
    private final ReattachController reattachController = new ReattachController();
    private final FlowCreationController flowCreation = new FlowCreationController();
    private final CopyPasteController copyPaste = new CopyPasteController();
    private final ConnectionRerouteController rerouteController = new ConnectionRerouteController();
    private final InlineEditController inlineEdit = new InlineEditController();
    private final ModuleNavigationController navController = new ModuleNavigationController();

    // Pan state
    private boolean panning;
    private boolean panMoved;
    private boolean spaceDown;
    private double dragStartX;
    private double dragStartY;

    // Mouse position tracking for cursor updates
    private double lastMouseX;
    private double lastMouseY;

    // Undo/redo
    private UndoManager undoManager;

    // Status change callback
    private Runnable onStatusChanged;

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
        if (!navController.isInsideModule()) {
            return editor.toModelDefinition(canvasState.toViewDef());
        }

        // Build current level's definition with its view
        ModelDefinition childDef = editor.toModelDefinition(canvasState.toViewDef());

        // Walk the stack from top (most recent parent) to bottom (root),
        // writing each child into its parent
        List<NavigationStack.Frame> frames = new ArrayList<>(navController.frames());
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
     * Regenerates connectors from the current model state and redraws.
     */
    private void regenerateAndRedraw() {
        connectors = editor.generateConnectors();
        invalidateLoopAnalysis();
        redraw();
    }

    // --- High-level mutation methods for PropertiesPanel ---
    // Each wraps saveUndoState → editor.setXxx → regenerateAndRedraw,
    // encapsulating the mutation protocol so callers don't need to know it.

    public void applyStockInitialValue(String name, double value) {
        saveUndoState();
        editor.setStockInitialValue(name, value);
        regenerateAndRedraw();
    }

    public void applyStockUnit(String name, String unit) {
        saveUndoState();
        editor.setStockUnit(name, unit);
        regenerateAndRedraw();
    }

    public void applyStockNegativeValuePolicy(String name, String policy) {
        saveUndoState();
        editor.setStockNegativeValuePolicy(name, policy);
        regenerateAndRedraw();
    }

    public void applyFlowEquation(String name, String equation) {
        saveUndoState();
        editor.setFlowEquation(name, equation);
        regenerateAndRedraw();
    }

    public void applyFlowTimeUnit(String name, String timeUnit) {
        saveUndoState();
        editor.setFlowTimeUnit(name, timeUnit);
        regenerateAndRedraw();
    }

    public void applyAuxEquation(String name, String equation) {
        saveUndoState();
        editor.setAuxEquation(name, equation);
        regenerateAndRedraw();
    }

    public void applyAuxUnit(String name, String unit) {
        saveUndoState();
        editor.setAuxUnit(name, unit);
        regenerateAndRedraw();
    }

    public void applyConstantValue(String name, double value) {
        saveUndoState();
        editor.setConstantValue(name, value);
        regenerateAndRedraw();
    }

    public void applyConstantUnit(String name, String unit) {
        saveUndoState();
        editor.setConstantUnit(name, unit);
        regenerateAndRedraw();
    }

    public void applyLookupTable(String name, LookupTableDef updated) {
        saveUndoState();
        editor.setLookupTable(name, updated);
        regenerateAndRedraw();
    }

    /**
     * Builds the render state for connection rerouting.
     */
    private CanvasRenderer.RerouteState rerouteRenderState() {
        if (!rerouteController.isActive() || !rerouteController.isDragStarted()) {
            return CanvasRenderer.RerouteState.IDLE;
        }
        return new CanvasRenderer.RerouteState(
                true,
                rerouteController.getAnchorX(),
                rerouteController.getAnchorY(),
                rerouteController.getRubberBandX(),
                rerouteController.getRubberBandY());
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
        inlineEdit.setOverlayPane(overlayPane);
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
        renderer.render(getGraphicsContext2D(), getWidth(), getHeight(),
                editor, connectors, flowCreation.getState(),
                reattachController.toRenderState(),
                rerouteRenderState(),
                marqueeController.toRenderState(),
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
            case PLACE_LOOKUP -> {
                name = editor.addLookup();
                type = ElementType.LOOKUP;
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

    /**
     * Copies the current selection to the clipboard.
     */
    private void copySelection() {
        if (editor == null) {
            return;
        }
        copyPaste.copy(canvasState, editor);
    }

    /**
     * Cuts the current selection: copies to clipboard, then deletes.
     */
    private void cutSelection() {
        if (editor == null || canvasState.getSelection().isEmpty()) {
            return;
        }
        copyPaste.copy(canvasState, editor);
        deleteSelected();
    }

    /**
     * Pastes clipboard contents, creating new elements offset from the originals.
     */
    private void pasteClipboard() {
        if (editor == null || !copyPaste.hasContent()) {
            return;
        }

        saveUndoState();

        List<String> pastedNames = copyPaste.paste(canvasState, editor);
        if (pastedNames.isEmpty()) {
            return;
        }

        canvasState.clearSelection();
        for (String name : pastedNames) {
            canvasState.addToSelection(name);
        }

        connectors = editor.generateConnectors();
        invalidateLoopAnalysis();
        redraw();
    }

    /**
     * Deletes the currently selected info link connection by removing the equation reference.
     */
    private void deleteSelectedConnection() {
        if (editor == null || selectedConnection == null) {
            return;
        }

        saveUndoState();
        editor.removeConnectionReference(selectedConnection.from(), selectedConnection.to());
        selectedConnection = null;
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

    private final InlineEditController.Callbacks inlineCallbacks =
            new InlineEditController.Callbacks() {
                @Override
                public void applyRename(String oldName, String newName) {
                    ModelCanvas.this.applyRename(oldName, newName);
                }

                @Override
                public void saveAndSetConstantValue(String name, double value) {
                    saveUndoState();
                    editor.setConstantValue(name, value);
                    redraw();
                }

                @Override
                public void saveAndSetFlowEquation(String name, String equation) {
                    saveUndoState();
                    editor.setFlowEquation(name, equation);
                    regenerateAndRedraw();
                }

                @Override
                public void saveAndSetAuxEquation(String name, String equation) {
                    saveUndoState();
                    editor.setAuxEquation(name, equation);
                    regenerateAndRedraw();
                }

                @Override
                public void postEdit() {
                    requestFocus();
                    updateCursor();
                }
            };

    /**
     * Starts inline editing for the given element on double-click.
     */
    private void startInlineEdit(String elementName) {
        inlineEdit.startEdit(elementName, canvasState, editor, viewport, inlineCallbacks);
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

    // --- Reattachment, marquee, resize, drag: delegated to controllers ---

    // --- Cursor ---

    /**
     * Updates the cursor shape based on the current interaction state.
     */
    private void updateCursor() {
        if (inlineEdit.isActive()) {
            return;
        }

        Cursor cursor;

        if (resizeController.isActive()) {
            cursor = ResizeController.cursorFor(resizeController.getHandle());
        } else if (reattachController.isActive() || rerouteController.isActive()
                || panning || dragController.isDragging()) {
            cursor = Cursor.CLOSED_HAND;
        } else if (marqueeController.isActive()) {
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
                cursor = ResizeController.cursorFor(handleHit.handle());
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
        if (inlineEdit.isActive()) {
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
                    reattachController.start(cloudHit, canvasState);
                    redraw();
                    updateCursor();
                    event.consume();
                    return;
                }
            }

            // Resize handle check: takes priority over move drag
            if (activeTool == CanvasToolBar.Tool.SELECT && !flowCreation.isPending()) {
                ResizeHandle.HandleHit handleHit = ResizeHandle.hitTest(canvasState, worldX, worldY);
                if (handleHit != null) {
                    resizeController.start(handleHit, canvasState);
                    updateCursor();
                    event.consume();
                    return;
                }
            }

            // Connection reroute: clicking near an endpoint of the selected connection
            if (activeTool == CanvasToolBar.Tool.SELECT && !flowCreation.isPending()
                    && selectedConnection != null) {
                ConnectionRerouteController.RerouteHit rerouteHit =
                        ConnectionRerouteController.hitTestEndpoint(
                                selectedConnection, canvasState, connectors, worldX, worldY);
                if (rerouteHit != null) {
                    rerouteController.prepare(rerouteHit);
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

                // Start drag for all selected elements
                dragController.start(hit, event.getX(), event.getY(), canvasState);
            } else {
                // No element hit — check for connection click
                ConnectionId connHit = HitTester.hitTestInfoLink(
                        canvasState, connectors, worldX, worldY);
                if (connHit != null) {
                    // Connection click: select connection, clear element selection
                    selectedConnection = connHit;
                    canvasState.clearSelection();
                } else {
                    // Empty space: clear connection selection, start marquee
                    selectedConnection = null;
                    marqueeController.start(worldX, worldY, canvasState, event.isShiftDown());
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

        if (marqueeController.isActive()) {
            marqueeController.drag(
                    viewport.toWorldX(event.getX()),
                    viewport.toWorldY(event.getY()), canvasState);
            redraw();
            event.consume();
            return;
        }

        if (reattachController.isActive()) {
            reattachController.drag(
                    viewport.toWorldX(event.getX()),
                    viewport.toWorldY(event.getY()));
            redraw();
            event.consume();
            return;
        }

        if (rerouteController.isActive()) {
            rerouteController.drag(
                    viewport.toWorldX(event.getX()),
                    viewport.toWorldY(event.getY()));
            redraw();
            event.consume();
            return;
        }

        if (resizeController.isActive()) {
            resizeController.drag(
                    viewport.toWorldX(event.getX()),
                    viewport.toWorldY(event.getY()),
                    canvasState, this::saveUndoState);
            redraw();
            event.consume();
            return;
        }

        if (panning) {
            panMoved = true;
            double screenDx = event.getX() - dragStartX;
            double screenDy = event.getY() - dragStartY;
            viewport.pan(screenDx, screenDy);
            dragStartX = event.getX();
            dragStartY = event.getY();
            redraw();
            event.consume();
            return;
        }

        if (dragController.isDragging()) {
            dragController.drag(event.getX(), event.getY(),
                    canvasState, viewport, this::saveUndoState);
            redraw();
            event.consume();
        }
    }

    private void handleMouseReleased(MouseEvent event) {
        if (marqueeController.isActive()) {
            marqueeController.end();
            redraw();
            updateCursor();
            event.consume();
            return;
        }

        if (reattachController.isActive()) {
            reattachController.complete(
                    viewport.toWorldX(event.getX()),
                    viewport.toWorldY(event.getY()),
                    canvasState, editor, this::saveUndoState);
            connectors = editor.generateConnectors();
            invalidateLoopAnalysis();
            redraw();
            updateCursor();
            event.consume();
            return;
        }

        if (rerouteController.isActive()) {
            boolean rerouted = rerouteController.complete(
                    viewport.toWorldX(event.getX()),
                    viewport.toWorldY(event.getY()),
                    canvasState, editor, this::saveUndoState);
            if (rerouted) {
                selectedConnection = null;
                connectors = editor.generateConnectors();
                invalidateLoopAnalysis();
            }
            redraw();
            updateCursor();
            event.consume();
            return;
        }

        if (resizeController.isActive()) {
            resizeController.end();
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

        dragController.end();
        panning = false;
        panMoved = false;
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

    /**
     * Selects a single element by name, clearing the current selection.
     * Used by the validation dialog to highlight a specific element.
     */
    public void selectElement(String name) {
        canvasState.clearSelection();
        canvasState.select(name);
        redraw();
    }

    private void handleKeyPressed(KeyEvent event) {
        // Guard: ignore key events while inline editor is active
        if (inlineEdit.isActive()) {
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
            if (selectedConnection != null && canvasState.getSelection().isEmpty()) {
                deleteSelectedConnection();
            } else {
                deleteSelected();
            }
            event.consume();
        } else if (event.isShortcutDown() && event.getCode() == KeyCode.A) {
            selectAll();
            event.consume();
        } else if (event.isShortcutDown() && event.getCode() == KeyCode.C) {
            copySelection();
            event.consume();
        } else if (event.isShortcutDown() && event.getCode() == KeyCode.X) {
            cutSelection();
            event.consume();
        } else if (event.isShortcutDown() && event.getCode() == KeyCode.V) {
            pasteClipboard();
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
                case DIGIT7 -> { switchTool(CanvasToolBar.Tool.PLACE_LOOKUP); event.consume(); }
                default -> { }
            }
        }
    }

    /**
     * Handles Escape key with priority chain: cancel reattachment, cancel pending flow,
     * reset tool to Select, clear selection.
     */
    private void handleEscape() {
        if (resizeController.isActive()) {
            resizeController.cancel(this::performUndo);
            redraw();
        } else if (marqueeController.isActive()) {
            marqueeController.cancel(canvasState);
            redraw();
        } else if (reattachController.isActive()) {
            reattachController.cancel();
            redraw();
        } else if (rerouteController.isActive()) {
            rerouteController.cancel();
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
        } else if (navController.isInsideModule()) {
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
     * Sets a callback invoked whenever the navigation state changes.
     */
    public void setOnNavigationChanged(Runnable callback) {
        navController.setOnNavigationChanged(callback);
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

        navController.push(new NavigationStack.Frame(
                moduleName, moduleIndex, editor, canvasState.toViewDef(),
                viewport.getTranslateX(), viewport.getTranslateY(),
                viewport.getScale(), undoManager, activeTool));

        ModelEditor moduleEditor = new ModelEditor();
        moduleEditor.loadFrom(module.definition());

        ViewDef moduleView;
        if (!module.definition().views().isEmpty()) {
            moduleView = module.definition().views().get(0);
        } else {
            moduleView = AutoLayout.layout(module.definition());
        }

        this.editor = moduleEditor;
        this.undoManager = new UndoManager();
        setModel(editor, moduleView);
        viewport.reset();

        if (toolBar != null) {
            toolBar.resetToSelect();
        } else {
            activeTool = CanvasToolBar.Tool.SELECT;
        }

        navController.fireNavigationChanged();
        fireStatusChanged();
    }

    /**
     * Navigates back to the parent level, writing the current level's definition
     * back into the parent's module.
     */
    public void navigateBack() {
        if (!navController.isInsideModule()) {
            return;
        }

        ModelDefinition childDef = editor.toModelDefinition(canvasState.toViewDef());
        NavigationStack.Frame frame = navController.pop();

        this.editor = frame.editor();
        this.undoManager = frame.undoManager();

        saveUndoState();
        editor.updateModuleDefinition(frame.moduleIndex(), childDef);

        canvasState.loadFrom(frame.viewSnapshot());
        viewport.restoreState(frame.viewportTranslateX(),
                frame.viewportTranslateY(), frame.viewportScale());

        if (toolBar != null) {
            toolBar.selectTool(frame.activeTool());
        } else {
            activeTool = frame.activeTool();
        }

        connectors = editor.generateConnectors();
        invalidateLoopAnalysis();
        redraw();

        navController.fireNavigationChanged();
        fireStatusChanged();
    }

    public void navigateToDepth(int targetDepth) {
        while (navController.depth() > targetDepth) {
            navigateBack();
        }
    }

    public boolean isInsideModule() {
        return navController.isInsideModule();
    }

    public List<String> getNavigationPath() {
        String rootName = navController.isInsideModule()
                ? navController.frames().get(0).editor().getModelName()
                : editor.getModelName();
        return navController.getPath(rootName);
    }

    public String getCurrentModuleName() {
        return navController.getCurrentModuleName();
    }

    public void clearNavigation() {
        navController.clear();
    }

    private void showElementContextMenu(String elementName, double screenX, double screenY) {
        navController.showContextMenu(this, elementName, canvasState, screenX, screenY,
                this::drillInto, this::openBindingsDialog, this::startInlineEdit);
    }

    private void openBindingsDialog(String moduleName) {
        navController.openBindingsDialog(moduleName, editor,
                this::saveUndoState, this::fireStatusChanged);
    }

}

package com.deathrayresearch.forrester.app.canvas;

import com.deathrayresearch.forrester.model.def.ConnectorRoute;
import com.deathrayresearch.forrester.model.def.ConstantDef;
import com.deathrayresearch.forrester.model.def.ElementType;
import com.deathrayresearch.forrester.model.def.ModelDefinition;
import com.deathrayresearch.forrester.model.def.ViewDef;

import javafx.scene.canvas.Canvas;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private boolean spaceDown;
    private double dragStartX;
    private double dragStartY;
    private String dragTarget;
    private final Map<String, double[]> dragStartPositions = new HashMap<>();

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
     */
    public ModelDefinition toModelDefinition() {
        return editor.toModelDefinition(canvasState.toViewDef());
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
     * Redraws the entire canvas by delegating to the CanvasRenderer.
     */
    private void redraw() {
        CanvasRenderer.ReattachState reattachState = reattaching
                ? new CanvasRenderer.ReattachState(true, reattachDiamondX, reattachDiamondY,
                        reattachRubberBandX, reattachRubberBandY)
                : CanvasRenderer.ReattachState.IDLE;
        renderer.render(getGraphicsContext2D(), getWidth(), getHeight(),
                editor, connectors, flowCreation.getState(), reattachState);
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
            default -> {
                return;
            }
        }

        canvasState.addElement(name, type, worldX, worldY);
        connectors = editor.generateConnectors();
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

        List<String> toDelete = new ArrayList<>(canvasState.getSelection());
        for (String name : toDelete) {
            editor.removeElement(name);
            canvasState.removeElement(name);
        }

        connectors = editor.generateConnectors();
        redraw();
    }

    // --- Two-click flow creation ---

    /**
     * Handles a click during PLACE_FLOW mode by delegating to the FlowCreationController.
     */
    private void handleFlowClick(double worldX, double worldY) {
        String name = flowCreation.handleClick(worldX, worldY, canvasState, editor);
        if (name != null) {
            connectors = editor.generateConnectors();
            canvasState.clearSelection();
            canvasState.select(name);
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

        if (type == ElementType.CONSTANT) {
            startConstantNameEdit(elementName, screenX, screenY, fieldWidth);
        } else {
            inlineEditor.open(screenX, screenY, elementName, fieldWidth, newName -> {
                if (newName != null && !newName.equals(elementName)
                        && ModelEditor.isValidName(newName)) {
                    applyRename(elementName, newName);
                }
                requestFocus();
            });
        }
    }

    /**
     * Starts editing a constant's name. On commit, proceeds to value editing.
     */
    private void startConstantNameEdit(String elementName, double screenX, double screenY,
                                       double fieldWidth) {
        inlineEditor.open(screenX, screenY, elementName, fieldWidth, newName -> {
            String effectiveName;
            if (newName != null && !newName.equals(elementName)
                    && ModelEditor.isValidName(newName)) {
                applyRename(elementName, newName);
                effectiveName = newName;
            } else {
                effectiveName = elementName;
            }
            // Chain to value editing
            startConstantValueEdit(effectiveName, screenX, screenY, fieldWidth);
        });
    }

    /**
     * Starts editing a constant's value after the name edit completes.
     */
    private void startConstantValueEdit(String constantName, double screenX, double screenY,
                                        double fieldWidth) {
        ConstantDef cd = findConstant(constantName);
        String currentValue = cd != null ? ElementRenderer.formatValue(cd.value()) : "0";

        // Position value editor slightly below name
        double valueScreenY = screenY + 16 * viewport.getScale();

        inlineEditor.open(screenX, valueScreenY, currentValue, fieldWidth, valueText -> {
            if (valueText != null && !valueText.isBlank()) {
                try {
                    double value = Double.parseDouble(valueText);
                    editor.setConstantValue(constantName, value);
                    redraw();
                } catch (NumberFormatException ignored) {
                    // Invalid number — ignore
                }
            }
            requestFocus();
        });
    }

    /**
     * Applies a rename to both the model editor and canvas state,
     * then regenerates connectors and redraws.
     */
    private void applyRename(String oldName, String newName) {
        if (!editor.renameElement(oldName, newName)) {
            return;
        }
        canvasState.renameElement(oldName, newName);
        connectors = editor.generateConnectors();
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
        String stockHit = FlowCreationController.hitTestStockOnly(worldX, worldY, canvasState);
        editor.reconnectFlow(reattachFlowName, reattachEnd, stockHit);
        connectors = editor.generateConnectors();
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

    // --- Event handlers ---

    private void handleScroll(ScrollEvent event) {
        double factor = event.getDeltaY() > 0 ? ZOOM_FACTOR : 1.0 / ZOOM_FACTOR;
        viewport.zoomAt(event.getX(), event.getY(), factor);
        redraw();
        event.consume();
    }

    private void handleMouseMoved(MouseEvent event) {
        if (flowCreation.isPending()) {
            flowCreation.updateRubberBand(
                    viewport.toWorldX(event.getX()),
                    viewport.toWorldY(event.getY()));
            redraw();
            event.consume();
        }
    }

    private void handleMousePressed(MouseEvent event) {
        // Guard: ignore mouse clicks while inline editor is active
        if (inlineEditor != null && inlineEditor.isActive()) {
            return;
        }

        requestFocus();
        dragStartX = event.getX();
        dragStartY = event.getY();

        // Pan: middle-drag, right-drag, or Space+left-drag
        if (event.getButton() == MouseButton.MIDDLE
                || event.getButton() == MouseButton.SECONDARY
                || (event.getButton() == MouseButton.PRIMARY && spaceDown)) {
            panning = true;
            event.consume();
            return;
        }

        if (event.getButton() == MouseButton.PRIMARY) {
            double worldX = viewport.toWorldX(event.getX());
            double worldY = viewport.toWorldY(event.getY());

            // Double-click: start inline editing (only in SELECT mode, not during pending flow)
            if (event.getClickCount() == 2
                    && activeTool == CanvasToolBar.Tool.SELECT
                    && !flowCreation.isPending()) {
                String hit = HitTester.hitTest(canvasState, worldX, worldY);
                if (hit != null) {
                    startInlineEdit(hit);
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
                    event.consume();
                    return;
                }
            }

            // PLACE_FLOW: two-click protocol
            if (activeTool == CanvasToolBar.Tool.PLACE_FLOW) {
                handleFlowClick(worldX, worldY);
                event.consume();
                return;
            }

            // Placement mode: other PLACE_* tools — click on empty space to create
            if (activeTool != CanvasToolBar.Tool.SELECT) {
                String hit = HitTester.hitTest(canvasState, worldX, worldY);
                if (hit == null) {
                    createElementAt(worldX, worldY);
                    event.consume();
                    return;
                }
            }

            // Select mode
            String hit = HitTester.hitTest(canvasState, worldX, worldY);

            if (hit != null) {
                if (event.isShiftDown()) {
                    canvasState.toggleSelection(hit);
                } else if (!canvasState.isSelected(hit)) {
                    canvasState.select(hit);
                }

                // Capture drag start positions for all selected elements
                dragTarget = hit;
                dragging = true;
                dragStartPositions.clear();
                for (String name : canvasState.getSelection()) {
                    dragStartPositions.put(name,
                            new double[]{canvasState.getX(name), canvasState.getY(name)});
                }
            } else {
                if (!event.isShiftDown()) {
                    canvasState.clearSelection();
                }
                dragTarget = null;
                dragging = false;
            }

            redraw();
            event.consume();
        }
    }

    private void handleMouseDragged(MouseEvent event) {
        if (reattaching) {
            reattachRubberBandX = viewport.toWorldX(event.getX());
            reattachRubberBandY = viewport.toWorldY(event.getY());
            redraw();
            event.consume();
            return;
        }

        double screenDx = event.getX() - dragStartX;
        double screenDy = event.getY() - dragStartY;

        if (panning) {
            viewport.pan(screenDx, screenDy);
            dragStartX = event.getX();
            dragStartY = event.getY();
            redraw();
            event.consume();
            return;
        }

        if (dragging && dragTarget != null) {
            // Convert screen-space drag delta to world-space delta
            double worldDx = screenDx / viewport.getScale();
            double worldDy = screenDy / viewport.getScale();

            for (Map.Entry<String, double[]> entry : dragStartPositions.entrySet()) {
                double[] startPos = entry.getValue();
                canvasState.setPosition(entry.getKey(),
                        startPos[0] + worldDx,
                        startPos[1] + worldDy);
            }
            redraw();
            event.consume();
        }
    }

    private void handleMouseReleased(MouseEvent event) {
        if (reattaching) {
            completeReattachment(
                    viewport.toWorldX(event.getX()),
                    viewport.toWorldY(event.getY()));
            event.consume();
            return;
        }

        dragging = false;
        panning = false;
        dragTarget = null;
        dragStartPositions.clear();
        event.consume();
    }

    private void handleKeyPressed(KeyEvent event) {
        // Guard: ignore key events while inline editor is active
        if (inlineEditor != null && inlineEditor.isActive()) {
            return;
        }

        if (event.getCode() == KeyCode.SPACE) {
            spaceDown = true;
            event.consume();
        } else if (event.getCode() == KeyCode.DELETE || event.getCode() == KeyCode.BACK_SPACE) {
            deleteSelected();
            event.consume();
        } else if (event.getCode() == KeyCode.ESCAPE) {
            if (reattaching) {
                cancelReattachment();
                redraw();
            } else if (flowCreation.isPending()) {
                flowCreation.cancel();
                redraw();
            } else {
                if (toolBar != null) {
                    toolBar.resetToSelect();
                }
                activeTool = CanvasToolBar.Tool.SELECT;
            }
            event.consume();
        }
    }

    private void handleKeyReleased(KeyEvent event) {
        if (event.getCode() == KeyCode.SPACE) {
            spaceDown = false;
            event.consume();
        }
    }

}

package com.deathrayresearch.forrester.app.canvas;

import com.deathrayresearch.forrester.model.def.ConnectorRoute;
import com.deathrayresearch.forrester.model.def.ConstantDef;
import com.deathrayresearch.forrester.model.def.FlowDef;
import com.deathrayresearch.forrester.model.def.ModelDefinition;
import com.deathrayresearch.forrester.model.def.ViewDef;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Canvas component that renders a model using the Layered Flow Diagram visual language.
 * Supports pan (Space+drag, middle/right drag), zoom (scroll wheel),
 * click-to-select, and drag-to-move.
 */
public class ModelCanvas extends Canvas {

    private static final double ZOOM_FACTOR = 1.1;

    private ModelDefinition definition;
    private ViewDef view;

    private final Viewport viewport = new Viewport();
    private final CanvasState canvasState = new CanvasState();

    // Drag/pan state
    private boolean dragging;
    private boolean panning;
    private boolean spaceDown;
    private double dragStartX;
    private double dragStartY;
    private String dragTarget;
    private final Map<String, double[]> dragStartPositions = new HashMap<>();

    public ModelCanvas() {
        setFocusTraversable(true);

        widthProperty().addListener(observable -> redraw());
        heightProperty().addListener(observable -> redraw());

        setOnScroll(this::handleScroll);
        setOnMousePressed(this::handleMousePressed);
        setOnMouseDragged(this::handleMouseDragged);
        setOnMouseReleased(this::handleMouseReleased);
        setOnKeyPressed(this::handleKeyPressed);
        setOnKeyReleased(this::handleKeyReleased);
    }

    /**
     * Sets the model and view data, triggering a redraw.
     */
    public void setModel(ModelDefinition definition, ViewDef view) {
        this.definition = definition;
        this.view = view;
        canvasState.loadFrom(view);
        redraw();
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
     * Redraws the entire canvas: background in screen space, then connections,
     * elements, and selection indicators in world space via the viewport transform.
     */
    private void redraw() {
        double w = getWidth();
        double h = getHeight();
        GraphicsContext gc = getGraphicsContext2D();

        // Background in screen space
        gc.clearRect(0, 0, w, h);
        gc.setFill(ColorPalette.BACKGROUND);
        gc.fillRect(0, 0, w, h);

        if (definition == null || view == null) {
            return;
        }

        // Build lookup for constant values
        Map<String, ConstantDef> constantMap = new HashMap<>();
        for (ConstantDef c : definition.constants()) {
            constantMap.put(c.name(), c);
        }

        // Build lookup for stock units
        Map<String, String> stockUnitMap = new HashMap<>();
        for (var s : definition.stocks()) {
            stockUnitMap.put(s.name(), s.unit());
        }

        // Apply viewport transform for world-space rendering
        gc.save();
        viewport.applyTo(gc);

        // 1. Draw connections first (behind elements)
        drawMaterialFlows(gc);
        drawInfoLinks(gc);

        // 2. Draw elements on top
        for (String name : canvasState.getDrawOrder()) {
            String type = canvasState.getType(name);
            double cx = canvasState.getX(name);
            double cy = canvasState.getY(name);

            if (type == null) {
                continue;
            }

            switch (type) {
                case "stock" -> {
                    String unit = stockUnitMap.get(name);
                    ElementRenderer.drawStock(gc, name, unit,
                            cx - LayoutMetrics.STOCK_WIDTH / 2,
                            cy - LayoutMetrics.STOCK_HEIGHT / 2,
                            LayoutMetrics.STOCK_WIDTH, LayoutMetrics.STOCK_HEIGHT);
                }
                case "flow" -> ElementRenderer.drawFlow(gc, name, cx, cy);
                case "aux" -> ElementRenderer.drawAux(gc, name,
                        cx - LayoutMetrics.AUX_WIDTH / 2,
                        cy - LayoutMetrics.AUX_HEIGHT / 2,
                        LayoutMetrics.AUX_WIDTH, LayoutMetrics.AUX_HEIGHT);
                case "constant" -> {
                    ConstantDef cd = constantMap.get(name);
                    double value = cd != null ? cd.value() : 0;
                    ElementRenderer.drawConstant(gc, name, value,
                            cx - LayoutMetrics.CONSTANT_WIDTH / 2,
                            cy - LayoutMetrics.CONSTANT_HEIGHT / 2,
                            LayoutMetrics.CONSTANT_WIDTH, LayoutMetrics.CONSTANT_HEIGHT);
                }
                default -> { }
            }
        }

        // 3. Draw selection indicators on top of everything
        for (String name : canvasState.getSelection()) {
            SelectionRenderer.drawSelectionIndicator(gc, canvasState, name);
        }

        gc.restore();
    }

    /**
     * Draws material flow arrows between stocks based on flow definitions.
     * Reads positions from CanvasState.
     */
    private void drawMaterialFlows(GraphicsContext gc) {
        for (FlowDef flow : definition.flows()) {
            double sourceX = Double.NaN;
            double sourceY = Double.NaN;
            double sinkX = Double.NaN;
            double sinkY = Double.NaN;

            if (flow.source() != null && canvasState.hasElement(flow.source())) {
                sourceX = canvasState.getX(flow.source()) + LayoutMetrics.STOCK_WIDTH / 2;
                sourceY = canvasState.getY(flow.source());
            }
            if (flow.sink() != null && canvasState.hasElement(flow.sink())) {
                sinkX = canvasState.getX(flow.sink()) - LayoutMetrics.STOCK_WIDTH / 2;
                sinkY = canvasState.getY(flow.sink());
            }

            ConnectionRenderer.drawMaterialFlow(gc, sourceX, sourceY, sinkX, sinkY, flow.name());
        }
    }

    /**
     * Draws info link dashed arrows based on connector routes.
     * Reads positions from CanvasState.
     */
    private void drawInfoLinks(GraphicsContext gc) {
        for (ConnectorRoute route : view.connectors()) {
            String fromName = route.from();
            String toName = route.to();

            if (!canvasState.hasElement(fromName) || !canvasState.hasElement(toName)) {
                continue;
            }

            double fromX = canvasState.getX(fromName);
            double fromY = canvasState.getY(fromName);
            double toX = canvasState.getX(toName);
            double toY = canvasState.getY(toName);

            String fromType = canvasState.getType(fromName);
            String toType = canvasState.getType(toName);

            double fromW = LayoutMetrics.widthFor(fromType) / 2;
            double fromH = LayoutMetrics.heightFor(fromType) / 2;
            double toW = LayoutMetrics.widthFor(toType) / 2;
            double toH = LayoutMetrics.heightFor(toType) / 2;

            double[] clippedFrom = clipToBorder(fromX, fromY, fromW, fromH, toX, toY);
            double[] clippedTo = clipToBorder(toX, toY, toW, toH, fromX, fromY);

            ConnectionRenderer.drawInfoLink(gc, clippedFrom[0], clippedFrom[1],
                    clippedTo[0], clippedTo[1]);
        }
    }

    // --- Event handlers ---

    private void handleScroll(ScrollEvent event) {
        double factor = event.getDeltaY() > 0 ? ZOOM_FACTOR : 1.0 / ZOOM_FACTOR;
        viewport.zoomAt(event.getX(), event.getY(), factor);
        redraw();
        event.consume();
    }

    private void handleMousePressed(MouseEvent event) {
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
        dragging = false;
        panning = false;
        dragTarget = null;
        dragStartPositions.clear();
        event.consume();
    }

    private void handleKeyPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.SPACE) {
            spaceDown = true;
            event.consume();
        }
    }

    private void handleKeyReleased(KeyEvent event) {
        if (event.getCode() == KeyCode.SPACE) {
            spaceDown = false;
            event.consume();
        }
    }

    /**
     * Clips a line from the center of a rectangle toward a target point,
     * returning the intersection with the rectangle border.
     */
    private static double[] clipToBorder(double cx, double cy, double halfW, double halfH,
                                         double targetX, double targetY) {
        double dx = targetX - cx;
        double dy = targetY - cy;
        if (dx == 0 && dy == 0) {
            return new double[]{cx, cy};
        }

        double scaleX = halfW > 0 ? Math.abs(halfW / dx) : Double.MAX_VALUE;
        double scaleY = halfH > 0 ? Math.abs(halfH / dy) : Double.MAX_VALUE;
        double scale = Math.min(scaleX, scaleY);

        return new double[]{cx + dx * scale, cy + dy * scale};
    }
}

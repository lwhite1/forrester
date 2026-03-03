package com.deathrayresearch.forrester.app.canvas;

import com.deathrayresearch.forrester.model.def.AuxDef;
import com.deathrayresearch.forrester.model.def.ConnectorRoute;
import com.deathrayresearch.forrester.model.def.ConstantDef;
import com.deathrayresearch.forrester.model.def.ElementType;
import com.deathrayresearch.forrester.model.def.FlowDef;
import com.deathrayresearch.forrester.model.def.StockDef;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Rendering coordinator for the model canvas.
 * Draws connections, elements, selection indicators, and rubber-band overlays.
 */
public class CanvasRenderer {

    private static final Color RUBBER_BAND_COLOR = Color.web("#4A90D9", 0.6);
    private static final Color STOCK_HOVER_COLOR = Color.web("#4A90D9", 0.4);
    private static final double RUBBER_BAND_DASH = 8;
    private static final double RUBBER_BAND_GAP = 4;

    /**
     * State for marquee (rubber-band) selection rendering.
     */
    public record MarqueeState(
            boolean active,
            double startX,
            double startY,
            double endX,
            double endY
    ) {
        static final MarqueeState IDLE = new MarqueeState(false, 0, 0, 0, 0);
    }

    /**
     * State for reattachment rubber-band rendering.
     */
    public record ReattachState(
            boolean active,
            double diamondX,
            double diamondY,
            double rubberBandX,
            double rubberBandY
    ) {
        static final ReattachState IDLE = new ReattachState(false, 0, 0, 0, 0);
    }

    private final CanvasState canvasState;
    private final Viewport viewport;

    public CanvasRenderer(CanvasState canvasState, Viewport viewport) {
        this.canvasState = canvasState;
        this.viewport = viewport;
    }

    /**
     * Renders the full canvas: background, connections, elements, selection, and overlays.
     */
    public void render(GraphicsContext gc, double width, double height,
                       ModelEditor editor, List<ConnectorRoute> connectors,
                       FlowCreationController.State flowState,
                       ReattachState reattachState,
                       MarqueeState marqueeState) {
        // Background in screen space
        gc.clearRect(0, 0, width, height);
        gc.setFill(ColorPalette.BACKGROUND);
        gc.fillRect(0, 0, width, height);

        if (editor == null) {
            return;
        }

        // Build lookup for constant values
        Map<String, ConstantDef> constantMap = new HashMap<>();
        for (ConstantDef c : editor.getConstants()) {
            constantMap.put(c.name(), c);
        }

        // Build lookup for stock units
        Map<String, String> stockUnitMap = new HashMap<>();
        for (StockDef s : editor.getStocks()) {
            stockUnitMap.put(s.name(), s.unit());
        }

        // Build lookup for flow equations
        Map<String, String> flowEquationMap = new HashMap<>();
        for (FlowDef f : editor.getFlows()) {
            flowEquationMap.put(f.name(), f.equation());
        }

        // Build lookup for auxiliary equations
        Map<String, String> auxEquationMap = new HashMap<>();
        for (AuxDef a : editor.getAuxiliaries()) {
            auxEquationMap.put(a.name(), a.equation());
        }

        // Apply viewport transform for world-space rendering
        gc.save();
        viewport.applyTo(gc);

        // 1. Draw connections first (behind elements)
        drawMaterialFlows(gc, editor);
        drawInfoLinks(gc, connectors);

        // 2. Draw elements on top
        for (String name : canvasState.getDrawOrder()) {
            ElementType type = canvasState.getType(name);
            double cx = canvasState.getX(name);
            double cy = canvasState.getY(name);

            if (type == null) {
                continue;
            }

            switch (type) {
                case STOCK -> {
                    String unit = stockUnitMap.get(name);
                    ElementRenderer.drawStock(gc, name, unit,
                            cx - LayoutMetrics.STOCK_WIDTH / 2,
                            cy - LayoutMetrics.STOCK_HEIGHT / 2,
                            LayoutMetrics.STOCK_WIDTH, LayoutMetrics.STOCK_HEIGHT);
                }
                case FLOW -> {
                    String flowEq = flowEquationMap.get(name);
                    ElementRenderer.drawFlow(gc, name, flowEq,
                            cx - LayoutMetrics.FLOW_INDICATOR_SIZE / 2,
                            cy - LayoutMetrics.FLOW_INDICATOR_SIZE / 2,
                            LayoutMetrics.FLOW_INDICATOR_SIZE, LayoutMetrics.FLOW_INDICATOR_SIZE);
                }
                case AUX -> {
                    String auxEq = auxEquationMap.get(name);
                    ElementRenderer.drawAux(gc, name, auxEq,
                            cx - LayoutMetrics.AUX_WIDTH / 2,
                            cy - LayoutMetrics.AUX_HEIGHT / 2,
                            LayoutMetrics.AUX_WIDTH, LayoutMetrics.AUX_HEIGHT);
                }
                case CONSTANT -> {
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

        // 4. Draw rubber-band line during pending flow creation
        if (flowState.pending()) {
            drawFlowRubberBand(gc, flowState);
        }

        // 5. Draw reattachment rubber-band
        if (reattachState.active()) {
            drawReattachRubberBand(gc, reattachState);
        }

        // 6. Draw marquee selection rectangle
        if (marqueeState.active()) {
            drawMarquee(gc, marqueeState);
        }

        gc.restore();
    }

    /**
     * Draws material flow arrows routed through the flow indicator (diamond).
     * Cloud positions are computed via {@link FlowEndpointCalculator#cloudPosition}
     * so that rendering and hit-testing use the same logic.
     */
    private void drawMaterialFlows(GraphicsContext gc, ModelEditor editor) {
        for (FlowDef flow : editor.getFlows()) {
            if (!canvasState.hasElement(flow.name())) {
                continue;
            }
            double midX = canvasState.getX(flow.name());
            double midY = canvasState.getY(flow.name());

            double sourceX;
            double sourceY;
            boolean sourceIsCloud;

            if (flow.source() != null && canvasState.hasElement(flow.source())) {
                double scx = canvasState.getX(flow.source());
                double scy = canvasState.getY(flow.source());
                double[] edge = clipToBorder(scx, scy,
                        LayoutMetrics.STOCK_WIDTH / 2, LayoutMetrics.STOCK_HEIGHT / 2,
                        midX, midY);
                sourceX = edge[0];
                sourceY = edge[1];
                sourceIsCloud = false;
            } else {
                double[] cloud = FlowEndpointCalculator.cloudPosition(
                        FlowEndpointCalculator.FlowEnd.SOURCE, flow, canvasState);
                sourceX = cloud != null ? cloud[0] : midX - LayoutMetrics.CLOUD_OFFSET;
                sourceY = cloud != null ? cloud[1] : midY;
                sourceIsCloud = true;
            }

            double sinkX;
            double sinkY;
            boolean sinkIsCloud;

            if (flow.sink() != null && canvasState.hasElement(flow.sink())) {
                double scx = canvasState.getX(flow.sink());
                double scy = canvasState.getY(flow.sink());
                double[] edge = clipToBorder(scx, scy,
                        LayoutMetrics.STOCK_WIDTH / 2, LayoutMetrics.STOCK_HEIGHT / 2,
                        midX, midY);
                sinkX = edge[0];
                sinkY = edge[1];
                sinkIsCloud = false;
            } else {
                double[] cloud = FlowEndpointCalculator.cloudPosition(
                        FlowEndpointCalculator.FlowEnd.SINK, flow, canvasState);
                sinkX = cloud != null ? cloud[0] : midX + LayoutMetrics.CLOUD_OFFSET;
                sinkY = cloud != null ? cloud[1] : midY;
                sinkIsCloud = true;
            }

            ConnectionRenderer.drawMaterialFlow(gc, sourceX, sourceY, midX, midY,
                    sinkX, sinkY, sourceIsCloud, sinkIsCloud);
        }
    }

    /**
     * Draws info link dashed arrows based on cached connector routes.
     */
    private void drawInfoLinks(GraphicsContext gc, List<ConnectorRoute> connectors) {
        for (ConnectorRoute route : connectors) {
            String fromName = route.from();
            String toName = route.to();

            if (!canvasState.hasElement(fromName) || !canvasState.hasElement(toName)) {
                continue;
            }

            double fromX = canvasState.getX(fromName);
            double fromY = canvasState.getY(fromName);
            double toX = canvasState.getX(toName);
            double toY = canvasState.getY(toName);

            ElementType fromType = canvasState.getType(fromName);
            ElementType toType = canvasState.getType(toName);

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

    /**
     * Draws a rubber-band line for flow creation.
     */
    private void drawFlowRubberBand(GraphicsContext gc, FlowCreationController.State state) {
        double startX = state.sourceX();
        double startY = state.sourceY();

        if (state.source() == null) {
            ConnectionRenderer.drawCloud(gc, startX, startY);
        }

        gc.setStroke(RUBBER_BAND_COLOR);
        gc.setLineWidth(2);
        gc.setLineDashes(RUBBER_BAND_DASH, RUBBER_BAND_GAP);
        gc.strokeLine(startX, startY, state.rubberBandEndX(), state.rubberBandEndY());
        gc.setLineDashes();

        String hoverStock = FlowCreationController.hitTestStockOnly(
                state.rubberBandEndX(), state.rubberBandEndY(), canvasState);
        if (hoverStock != null) {
            drawStockHoverHighlight(gc, hoverStock);
        }
    }

    /**
     * Draws a rubber-band line for flow endpoint reattachment.
     * Shows a stock highlight when hovering a stock, or a cloud preview on empty space.
     */
    private void drawReattachRubberBand(GraphicsContext gc, ReattachState state) {
        gc.setStroke(RUBBER_BAND_COLOR);
        gc.setLineWidth(2);
        gc.setLineDashes(RUBBER_BAND_DASH, RUBBER_BAND_GAP);
        gc.strokeLine(state.diamondX(), state.diamondY(), state.rubberBandX(), state.rubberBandY());
        gc.setLineDashes();

        String hoverStock = FlowCreationController.hitTestStockOnly(
                state.rubberBandX(), state.rubberBandY(), canvasState);
        if (hoverStock != null) {
            drawStockHoverHighlight(gc, hoverStock);
        } else {
            // Preview cloud at drop position when not hovering a stock
            ConnectionRenderer.drawCloud(gc, state.rubberBandX(), state.rubberBandY());
        }
    }

    /**
     * Draws a dashed highlight rectangle around a stock element.
     */
    private void drawStockHoverHighlight(GraphicsContext gc, String stockName) {
        double sx = canvasState.getX(stockName);
        double sy = canvasState.getY(stockName);
        double halfW = LayoutMetrics.STOCK_WIDTH / 2 + 4;
        double halfH = LayoutMetrics.STOCK_HEIGHT / 2 + 4;

        gc.setStroke(STOCK_HOVER_COLOR);
        gc.setLineWidth(2.5);
        gc.setLineDashes(6, 3);
        gc.strokeRect(sx - halfW, sy - halfH, halfW * 2, halfH * 2);
        gc.setLineDashes();
    }

    /**
     * Draws the marquee (rubber-band) selection rectangle.
     */
    private void drawMarquee(GraphicsContext gc, MarqueeState state) {
        double x = Math.min(state.startX(), state.endX());
        double y = Math.min(state.startY(), state.endY());
        double w = Math.abs(state.endX() - state.startX());
        double h = Math.abs(state.endY() - state.startY());

        gc.setFill(Color.web("#4A90D9", 0.1));
        gc.fillRect(x, y, w, h);

        gc.setStroke(RUBBER_BAND_COLOR);
        gc.setLineWidth(1);
        gc.setLineDashes(6, 3);
        gc.strokeRect(x, y, w, h);
        gc.setLineDashes();
    }

    /**
     * Clips a line from the center of a rectangle toward a target point,
     * returning the intersection with the rectangle border.
     */
    static double[] clipToBorder(double cx, double cy, double halfW, double halfH,
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

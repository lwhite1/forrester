package systems.courant.forrester.app.canvas;

import systems.courant.forrester.model.def.ElementType;
import systems.courant.forrester.model.graph.CausalTraceAnalysis;
import systems.courant.forrester.model.graph.CausalTraceAnalysis.TraceDirection;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Draws visual highlights for elements and edges in a causal trace.
 * Uses blue for upstream traces and orange for downstream traces,
 * with progressive opacity fading by BFS depth.
 */
public final class CausalTraceRenderer {

    private static final double GLOW_PADDING = 6;
    private static final double GLOW_LINE_WIDTH = 2.5;
    private static final double EDGE_LINE_WIDTH = 2.5;
    private static final double ORIGIN_LINE_WIDTH = 3.0;

    private CausalTraceRenderer() {
    }

    /**
     * Returns the trace color for the given direction.
     */
    public static Color traceColor(TraceDirection direction) {
        return direction == TraceDirection.UPSTREAM
                ? ColorPalette.TRACE_UPSTREAM
                : ColorPalette.TRACE_DOWNSTREAM;
    }

    /**
     * Draws a colored glow around a traced element with depth-based opacity.
     */
    public static void drawTraceHighlight(GraphicsContext gc, CanvasState state,
                                           String name, CausalTraceAnalysis trace) {
        ElementType type = state.getType(name).orElse(null);
        double cx = state.getX(name);
        double cy = state.getY(name);

        if (type == null || Double.isNaN(cx) || Double.isNaN(cy)) {
            return;
        }

        int depth = trace.depthOf(name);
        double opacity = trace.opacityForDepth(depth);
        boolean isOrigin = name.equals(trace.origin());

        Color baseColor = isOrigin ? ColorPalette.TRACE_ORIGIN : traceColor(trace.direction());
        Color strokeColor = Color.color(baseColor.getRed(), baseColor.getGreen(),
                baseColor.getBlue(), opacity * 0.8);
        Color fillColor = Color.color(baseColor.getRed(), baseColor.getGreen(),
                baseColor.getBlue(), opacity * 0.08);

        gc.setStroke(strokeColor);
        gc.setLineWidth(isOrigin ? ORIGIN_LINE_WIDTH : GLOW_LINE_WIDTH);
        gc.setLineDashes();

        if (type == ElementType.FLOW) {
            double half = LayoutMetrics.FLOW_INDICATOR_SIZE / 2 + GLOW_PADDING;
            double[] xPoints = {cx, cx + half, cx, cx - half};
            double[] yPoints = {cy - half, cy, cy + half, cy};
            gc.setFill(fillColor);
            gc.fillPolygon(xPoints, yPoints, 4);
            gc.strokePolygon(xPoints, yPoints, 4);
        } else {
            double halfW = LayoutMetrics.effectiveWidth(state, name) / 2 + GLOW_PADDING;
            double halfH = LayoutMetrics.effectiveHeight(state, name) / 2 + GLOW_PADDING;
            double x = cx - halfW;
            double y = cy - halfH;
            gc.setFill(fillColor);
            gc.fillRect(x, y, halfW * 2, halfH * 2);
            gc.strokeRect(x, y, halfW * 2, halfH * 2);
        }
    }

    /**
     * Draws a highlighted trace edge (straight line) with depth-based opacity.
     */
    public static void drawTraceEdge(GraphicsContext gc,
                                      double fromX, double fromY,
                                      double toX, double toY,
                                      double opacity, TraceDirection direction) {
        Color base = traceColor(direction);
        gc.setStroke(Color.color(base.getRed(), base.getGreen(), base.getBlue(), opacity * 0.7));
        gc.setLineWidth(EDGE_LINE_WIDTH);
        gc.setLineDashes();
        gc.strokeLine(fromX, fromY, toX, toY);
    }

    /**
     * Draws a highlighted trace edge (curved) with depth-based opacity.
     */
    public static void drawTraceEdgeCurved(GraphicsContext gc,
                                            double fromX, double fromY,
                                            double cpX, double cpY,
                                            double toX, double toY,
                                            double opacity, TraceDirection direction) {
        Color base = traceColor(direction);
        gc.setStroke(Color.color(base.getRed(), base.getGreen(), base.getBlue(), opacity * 0.7));
        gc.setLineWidth(EDGE_LINE_WIDTH);
        gc.setLineDashes();

        gc.beginPath();
        gc.moveTo(fromX, fromY);
        int segments = 30;
        for (int i = 1; i <= segments; i++) {
            double t = (double) i / segments;
            double[] pt = CausalLinkGeometry.evaluate(fromX, fromY, cpX, cpY, toX, toY, t);
            gc.lineTo(pt[0], pt[1]);
        }
        gc.stroke();
    }
}

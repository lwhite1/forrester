package systems.courant.sd.app.canvas.renderers;

import systems.courant.sd.model.graph.CausalTraceAnalysis;
import systems.courant.sd.model.graph.CausalTraceAnalysis.TraceDirection;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import systems.courant.sd.app.canvas.CanvasState;
import systems.courant.sd.app.canvas.CausalLinkGeometry;
import systems.courant.sd.app.canvas.ColorPalette;

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
        int depth = trace.depthOf(name);
        double opacity = trace.opacityForDepth(depth);
        boolean isOrigin = name.equals(trace.origin());

        Color baseColor = isOrigin ? ColorPalette.TRACE_ORIGIN : traceColor(trace.direction());
        Color strokeColor = Color.color(baseColor.getRed(), baseColor.getGreen(),
                baseColor.getBlue(), opacity * 0.8);
        Color fillColor = Color.color(baseColor.getRed(), baseColor.getGreen(),
                baseColor.getBlue(), opacity * 0.08);

        gc.setLineDashes();
        OutlineGeometry.drawElementOutline(gc, state, name, GLOW_PADDING,
                fillColor, strokeColor, isOrigin ? ORIGIN_LINE_WIDTH : GLOW_LINE_WIDTH);
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

        CausalLinkGeometry.strokeArcCurve(gc, fromX, fromY, cpX, cpY, toX, toY, 1.0);
    }
}

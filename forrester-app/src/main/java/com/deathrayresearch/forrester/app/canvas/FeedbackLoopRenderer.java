package com.deathrayresearch.forrester.app.canvas;

import com.deathrayresearch.forrester.model.def.ElementType;

import javafx.scene.canvas.GraphicsContext;

/**
 * Draws visual highlights for elements and edges that participate in feedback loops.
 * Follows the same static-method pattern as {@link SelectionRenderer}.
 */
public final class FeedbackLoopRenderer {

    private static final double GLOW_PADDING = 6;
    private static final double GLOW_LINE_WIDTH = 2.5;
    private static final double EDGE_LINE_WIDTH = 2.5;

    private FeedbackLoopRenderer() {
    }

    /**
     * Draws a colored glow/outline around a loop participant element.
     * Uses a solid border in the loop highlight color with a subtle fill.
     */
    public static void drawLoopHighlight(GraphicsContext gc, CanvasState state, String name) {
        ElementType type = state.getType(name);
        double cx = state.getX(name);
        double cy = state.getY(name);

        if (type == null || Double.isNaN(cx) || Double.isNaN(cy)) {
            return;
        }

        gc.setStroke(ColorPalette.LOOP_HIGHLIGHT);
        gc.setLineWidth(GLOW_LINE_WIDTH);
        gc.setLineDashes();

        if (type == ElementType.FLOW) {
            double half = LayoutMetrics.FLOW_INDICATOR_SIZE / 2 + GLOW_PADDING;
            double[] xPoints = {cx, cx + half, cx, cx - half};
            double[] yPoints = {cy - half, cy, cy + half, cy};

            gc.setFill(ColorPalette.LOOP_FILL);
            gc.fillPolygon(xPoints, yPoints, 4);
            gc.strokePolygon(xPoints, yPoints, 4);
        } else {
            double halfW = LayoutMetrics.widthFor(type) / 2 + GLOW_PADDING;
            double halfH = LayoutMetrics.heightFor(type) / 2 + GLOW_PADDING;
            double x = cx - halfW;
            double y = cy - halfH;
            double w = halfW * 2;
            double h = halfH * 2;

            gc.setFill(ColorPalette.LOOP_FILL);
            gc.fillRect(x, y, w, h);
            gc.strokeRect(x, y, w, h);
        }
    }

    /**
     * Draws a thicker, colored version of an info link for edges within feedback loops.
     * Should be drawn before normal info links so it appears as a glow behind them.
     */
    public static void drawLoopEdge(GraphicsContext gc,
                                    double fromX, double fromY,
                                    double toX, double toY) {
        gc.setStroke(ColorPalette.LOOP_EDGE);
        gc.setLineWidth(EDGE_LINE_WIDTH);
        gc.setLineDashes();
        gc.strokeLine(fromX, fromY, toX, toY);
    }
}

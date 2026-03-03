package com.deathrayresearch.forrester.app.canvas;

import javafx.geometry.VPos;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.text.TextAlignment;

/**
 * Static methods for drawing material flows and info links between elements.
 */
public final class ConnectionRenderer {

    private ConnectionRenderer() {
    }

    /**
     * Draws a material flow: thick solid line with large arrowhead at the sink.
     * If sourceX/sourceY is NaN, draws a cloud at the source end.
     * If sinkX/sinkY is NaN, draws a cloud at the sink end.
     */
    public static void drawMaterialFlow(GraphicsContext gc,
                                        double sourceX, double sourceY,
                                        double sinkX, double sinkY,
                                        String flowName) {
        boolean hasSource = !Double.isNaN(sourceX);
        boolean hasSink = !Double.isNaN(sinkX);

        double startX = hasSource ? sourceX : sinkX - 80;
        double startY = hasSource ? sourceY : sinkY;
        double endX = hasSink ? sinkX : sourceX + 80;
        double endY = hasSink ? sinkY : sourceY;

        // Draw cloud at missing end
        if (!hasSource) {
            drawCloud(gc, startX, startY);
        }
        if (!hasSink) {
            drawCloud(gc, endX, endY);
        }

        // Main line
        gc.setStroke(ColorPalette.MATERIAL_FLOW);
        gc.setLineWidth(LayoutMetrics.MATERIAL_FLOW_WIDTH);
        gc.setLineDashes();
        gc.strokeLine(startX, startY, endX, endY);

        // Arrowhead at sink
        if (hasSink) {
            drawArrowhead(gc, startX, startY, endX, endY,
                    LayoutMetrics.ARROWHEAD_LENGTH, LayoutMetrics.ARROWHEAD_WIDTH,
                    ColorPalette.MATERIAL_FLOW);
        }

        // Flow name label at midpoint
        if (flowName != null) {
            double midX = (startX + endX) / 2;
            double midY = (startY + endY) / 2;
            gc.setFill(ColorPalette.TEXT);
            gc.setFont(LayoutMetrics.FLOW_NAME_FONT);
            gc.setTextAlign(TextAlignment.CENTER);
            gc.setTextBaseline(VPos.BOTTOM);
            gc.fillText(flowName, midX, midY - 6);
        }
    }

    /**
     * Draws an info link: thin dashed line with small arrowhead.
     */
    public static void drawInfoLink(GraphicsContext gc,
                                    double fromX, double fromY,
                                    double toX, double toY) {
        gc.setStroke(ColorPalette.INFO_LINK);
        gc.setLineWidth(LayoutMetrics.INFO_LINK_WIDTH);
        gc.setLineDashes(LayoutMetrics.INFO_LINK_DASH_LENGTH, LayoutMetrics.INFO_LINK_DASH_GAP);
        gc.strokeLine(fromX, fromY, toX, toY);
        gc.setLineDashes();

        drawArrowhead(gc, fromX, fromY, toX, toY,
                LayoutMetrics.INFO_ARROWHEAD_LENGTH, LayoutMetrics.INFO_ARROWHEAD_WIDTH,
                ColorPalette.INFO_LINK);
    }

    /**
     * Draws a filled arrowhead pointing from (fromX,fromY) toward (toX,toY),
     * with its tip at (toX,toY).
     */
    private static void drawArrowhead(GraphicsContext gc,
                                      double fromX, double fromY,
                                      double toX, double toY,
                                      double length, double width,
                                      javafx.scene.paint.Color color) {
        double dx = toX - fromX;
        double dy = toY - fromY;
        double dist = Math.sqrt(dx * dx + dy * dy);
        if (dist < 1) {
            return;
        }

        double ux = dx / dist;
        double uy = dy / dist;

        // Base of the arrowhead
        double baseX = toX - ux * length;
        double baseY = toY - uy * length;

        // Perpendicular offset
        double perpX = -uy * width / 2;
        double perpY = ux * width / 2;

        double[] xPoints = {toX, baseX + perpX, baseX - perpX};
        double[] yPoints = {toY, baseY + perpY, baseY - perpY};

        gc.setFill(color);
        gc.fillPolygon(xPoints, yPoints, 3);
    }

    /**
     * Draws a cloud symbol at the given world coordinates.
     * Public delegate for use by other renderers (e.g. rubber-band drawing).
     */
    public static void drawCloudAt(GraphicsContext gc, double cx, double cy) {
        drawCloud(gc, cx, cy);
    }

    /**
     * Draws a small cloud symbol representing an external source/sink.
     */
    private static void drawCloud(GraphicsContext gc, double cx, double cy) {
        double r = LayoutMetrics.CLOUD_RADIUS;
        gc.setStroke(ColorPalette.CLOUD);
        gc.setLineWidth(1.5);
        gc.setLineDashes();
        gc.strokeOval(cx - r, cy - r, r * 2, r * 2);

        // Draw a small "~" inside to distinguish from a plain circle
        gc.setFill(ColorPalette.CLOUD);
        gc.setFont(LayoutMetrics.BADGE_FONT);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.fillText("~", cx, cy);
    }
}

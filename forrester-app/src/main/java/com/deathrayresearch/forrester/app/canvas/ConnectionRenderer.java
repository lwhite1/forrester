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

    private static final double CLOUD_OFFSET = 80;

    /**
     * Draws a material flow routed through the flow indicator (diamond) position.
     * The path is: source → diamond → sink, with clouds at missing endpoints.
     *
     * @param sourceX  source stock edge X (NaN if cloud)
     * @param sourceY  source stock edge Y (NaN if cloud)
     * @param midX     flow indicator (diamond) center X
     * @param midY     flow indicator (diamond) center Y
     * @param sinkX    sink stock edge X (NaN if cloud)
     * @param sinkY    sink stock edge Y (NaN if cloud)
     */
    public static void drawMaterialFlow(GraphicsContext gc,
                                        double sourceX, double sourceY,
                                        double midX, double midY,
                                        double sinkX, double sinkY) {
        boolean hasSource = !Double.isNaN(sourceX);
        boolean hasSink = !Double.isNaN(sinkX);

        // Compute cloud positions relative to diamond when stock is missing
        double startX;
        double startY;
        if (hasSource) {
            startX = sourceX;
            startY = sourceY;
        } else {
            double dx = hasSink ? midX - sinkX : -CLOUD_OFFSET;
            double dy = hasSink ? midY - sinkY : 0;
            double dist = Math.sqrt(dx * dx + dy * dy);
            if (dist < 1) {
                dx = -CLOUD_OFFSET;
                dy = 0;
                dist = CLOUD_OFFSET;
            }
            startX = midX + dx / dist * CLOUD_OFFSET;
            startY = midY + dy / dist * CLOUD_OFFSET;
        }

        double endX;
        double endY;
        if (hasSink) {
            endX = sinkX;
            endY = sinkY;
        } else {
            double dx = hasSource ? midX - sourceX : CLOUD_OFFSET;
            double dy = hasSource ? midY - sourceY : 0;
            double dist = Math.sqrt(dx * dx + dy * dy);
            if (dist < 1) {
                dx = CLOUD_OFFSET;
                dy = 0;
                dist = CLOUD_OFFSET;
            }
            endX = midX + dx / dist * CLOUD_OFFSET;
            endY = midY + dy / dist * CLOUD_OFFSET;
        }

        // Draw clouds at missing endpoints
        if (!hasSource) {
            drawCloud(gc, startX, startY);
        }
        if (!hasSink) {
            drawCloud(gc, endX, endY);
        }

        // Source → diamond segment
        gc.setStroke(ColorPalette.MATERIAL_FLOW);
        gc.setLineWidth(LayoutMetrics.MATERIAL_FLOW_WIDTH);
        gc.setLineDashes();
        gc.strokeLine(startX, startY, midX, midY);

        // Diamond → sink segment
        gc.strokeLine(midX, midY, endX, endY);

        // Arrowhead at sink end
        drawArrowhead(gc, midX, midY, endX, endY,
                LayoutMetrics.ARROWHEAD_LENGTH, LayoutMetrics.ARROWHEAD_WIDTH,
                ColorPalette.MATERIAL_FLOW);
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

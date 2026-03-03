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
     * Draws a material flow routed through the flow indicator (diamond) position.
     * The path is: source → diamond → sink, with clouds drawn at endpoints marked as clouds.
     * All coordinates must be concrete (no NaN) — the caller is responsible for computing
     * cloud positions via {@link FlowEndpointCalculator#cloudPosition}.
     *
     * @param sourceX       source endpoint X
     * @param sourceY       source endpoint Y
     * @param midX          flow indicator (diamond) center X
     * @param midY          flow indicator (diamond) center Y
     * @param sinkX         sink endpoint X
     * @param sinkY         sink endpoint Y
     * @param sourceIsCloud true if the source endpoint is a cloud (disconnected)
     * @param sinkIsCloud   true if the sink endpoint is a cloud (disconnected)
     */
    public static void drawMaterialFlow(GraphicsContext gc,
                                        double sourceX, double sourceY,
                                        double midX, double midY,
                                        double sinkX, double sinkY,
                                        boolean sourceIsCloud, boolean sinkIsCloud) {
        // Draw clouds at disconnected endpoints
        if (sourceIsCloud) {
            drawCloud(gc, sourceX, sourceY);
        }
        if (sinkIsCloud) {
            drawCloud(gc, sinkX, sinkY);
        }

        // Source → diamond segment
        gc.setStroke(ColorPalette.MATERIAL_FLOW);
        gc.setLineWidth(LayoutMetrics.MATERIAL_FLOW_WIDTH);
        gc.setLineDashes();
        gc.strokeLine(sourceX, sourceY, midX, midY);

        // Diamond → sink segment
        gc.strokeLine(midX, midY, sinkX, sinkY);

        // Arrowhead at sink end
        drawArrowhead(gc, midX, midY, sinkX, sinkY,
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

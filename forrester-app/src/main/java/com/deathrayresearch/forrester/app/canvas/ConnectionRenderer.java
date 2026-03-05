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

        // Clip pipe endpoints to cloud borders so the pipe stops at the
        // cloud edge rather than running through to its center
        double pipeSourceX = sourceX;
        double pipeSourceY = sourceY;
        if (sourceIsCloud) {
            double dx = midX - sourceX;
            double dy = midY - sourceY;
            double dist = Math.sqrt(dx * dx + dy * dy);
            if (dist > 1) {
                pipeSourceX = sourceX + dx / dist * LayoutMetrics.CLOUD_RADIUS;
                pipeSourceY = sourceY + dy / dist * LayoutMetrics.CLOUD_RADIUS;
            }
        }

        double pipeSinkX = sinkX;
        double pipeSinkY = sinkY;
        if (sinkIsCloud) {
            double dx = midX - sinkX;
            double dy = midY - sinkY;
            double dist = Math.sqrt(dx * dx + dy * dy);
            if (dist > 1) {
                pipeSinkX = sinkX + dx / dist * LayoutMetrics.CLOUD_RADIUS;
                pipeSinkY = sinkY + dy / dist * LayoutMetrics.CLOUD_RADIUS;
            }
        }

        // Source → diamond segment (no arrowhead — draw full length)
        gc.setStroke(ColorPalette.MATERIAL_FLOW);
        gc.setLineWidth(LayoutMetrics.MATERIAL_FLOW_WIDTH);
        gc.setLineDashes();
        gc.strokeLine(pipeSourceX, pipeSourceY, midX, midY);

        // Diamond → sink segment: stop line at arrowhead base
        double sinkDx = pipeSinkX - midX;
        double sinkDy = pipeSinkY - midY;
        double sinkDist = Math.sqrt(sinkDx * sinkDx + sinkDy * sinkDy);
        double lineEndX = pipeSinkX;
        double lineEndY = pipeSinkY;
        if (sinkDist > LayoutMetrics.ARROWHEAD_LENGTH) {
            double ux = sinkDx / sinkDist;
            double uy = sinkDy / sinkDist;
            lineEndX = pipeSinkX - ux * LayoutMetrics.ARROWHEAD_LENGTH;
            lineEndY = pipeSinkY - uy * LayoutMetrics.ARROWHEAD_LENGTH;
        }
        gc.strokeLine(midX, midY, lineEndX, lineEndY);

        // Arrowhead fills the gap from lineEnd to pipeSink
        drawArrowhead(gc, midX, midY, pipeSinkX, pipeSinkY,
                LayoutMetrics.ARROWHEAD_LENGTH, LayoutMetrics.ARROWHEAD_WIDTH,
                ColorPalette.MATERIAL_FLOW);
    }

    /**
     * Draws an info link: thin dashed line with small arrowhead.
     */
    public static void drawInfoLink(GraphicsContext gc,
                                    double fromX, double fromY,
                                    double toX, double toY) {
        // Stop line at arrowhead base so it doesn't extend behind the arrowhead
        double dx = toX - fromX;
        double dy = toY - fromY;
        double dist = Math.sqrt(dx * dx + dy * dy);
        double lineToX = toX;
        double lineToY = toY;
        if (dist > LayoutMetrics.INFO_ARROWHEAD_LENGTH) {
            double ux = dx / dist;
            double uy = dy / dist;
            lineToX = toX - ux * LayoutMetrics.INFO_ARROWHEAD_LENGTH;
            lineToY = toY - uy * LayoutMetrics.INFO_ARROWHEAD_LENGTH;
        }

        gc.setStroke(ColorPalette.INFO_LINK);
        gc.setLineWidth(LayoutMetrics.INFO_LINK_WIDTH);
        gc.setLineDashes(LayoutMetrics.INFO_LINK_DASH_LENGTH, LayoutMetrics.INFO_LINK_DASH_GAP);
        gc.strokeLine(fromX, fromY, lineToX, lineToY);
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
     * Draws a small cloud symbol representing an external source/sink.
     */
    public static void drawCloud(GraphicsContext gc, double cx, double cy) {
        double r = LayoutMetrics.CLOUD_RADIUS;
        gc.setStroke(ColorPalette.CLOUD);
        gc.setLineWidth(LayoutMetrics.CLOUD_LINE_WIDTH);
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

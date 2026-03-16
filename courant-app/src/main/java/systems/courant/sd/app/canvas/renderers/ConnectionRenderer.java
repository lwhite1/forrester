package systems.courant.sd.app.canvas.renderers;

import systems.courant.sd.model.def.CausalLinkDef;

import javafx.geometry.VPos;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import systems.courant.sd.app.canvas.CausalLinkGeometry;
import systems.courant.sd.app.canvas.ColorPalette;
import systems.courant.sd.app.canvas.FlowEndpointCalculator;
import systems.courant.sd.app.canvas.LayoutMetrics;

/**
 * Static methods for drawing material flows and info links between elements.
 */
public final class ConnectionRenderer {

    private ConnectionRenderer() {
    }

    /**
     * Draws a material flow routed through the flow indicator (diamond) position,
     * using the default color.
     */
    public static void drawMaterialFlow(GraphicsContext gc,
                                        double sourceX, double sourceY,
                                        double midX, double midY,
                                        double sinkX, double sinkY,
                                        boolean sourceIsCloud, boolean sinkIsCloud) {
        drawMaterialFlow(gc, sourceX, sourceY, midX, midY, sinkX, sinkY,
                sourceIsCloud, sinkIsCloud, ColorPalette.MATERIAL_FLOW);
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
     * @param color         the color for the flow line and arrowhead
     */
    public static void drawMaterialFlow(GraphicsContext gc,
                                        double sourceX, double sourceY,
                                        double midX, double midY,
                                        double sinkX, double sinkY,
                                        boolean sourceIsCloud, boolean sinkIsCloud,
                                        Color color) {
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
        gc.setStroke(color);
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
                color);
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
     * Draws a causal link as a quadratic Bézier curve with arrowhead and polarity label.
     */
    public static void drawCausalLink(GraphicsContext gc,
                                      double fromX, double fromY,
                                      double toX, double toY,
                                      CausalLinkGeometry.ControlPoint cp,
                                      CausalLinkDef.Polarity polarity) {
        // Compute arrowhead placement along the curve
        double[] ah = CausalLinkGeometry.arrowheadPoint(fromX, fromY, cp.x(), cp.y(),
                toX, toY, LayoutMetrics.CAUSAL_ARROWHEAD_LENGTH);
        double tipX = ah[0];
        double tipY = ah[1];
        double tanX = ah[2];
        double tanY = ah[3];
        double stopT = ah[4];

        // Draw the curved line, stopping at the arrowhead base
        Color linkColor = polarity == CausalLinkDef.Polarity.UNKNOWN
                ? ColorPalette.CAUSAL_UNKNOWN : ColorPalette.CAUSAL_LINK;
        gc.setStroke(linkColor);
        gc.setLineWidth(LayoutMetrics.CAUSAL_LINK_WIDTH);
        if (polarity == CausalLinkDef.Polarity.UNKNOWN) {
            gc.setLineDashes(4, 3);
        } else {
            gc.setLineDashes();
        }

        gc.beginPath();
        gc.moveTo(fromX, fromY);
        // Draw the curve using sampled line segments for precision
        int segments = 30;
        for (int i = 1; i <= segments; i++) {
            double t = stopT * i / segments;
            double[] pt = CausalLinkGeometry.evaluate(fromX, fromY, cp.x(), cp.y(), toX, toY, t);
            gc.lineTo(pt[0], pt[1]);
        }
        gc.stroke();
        gc.setLineDashes();

        // Arrowhead oriented along the curve tangent at the tip
        drawArrowheadFromTangent(gc, tipX, tipY, tanX, tanY,
                LayoutMetrics.CAUSAL_ARROWHEAD_LENGTH, LayoutMetrics.CAUSAL_ARROWHEAD_WIDTH,
                linkColor);

        // Polarity label near the arrowhead, offset perpendicular to the curve tangent
        double dx = toX - fromX;
        double dy = toY - fromY;
        double dist = Math.sqrt(dx * dx + dy * dy);
        if (dist > 1) {
            Color labelColor = switch (polarity) {
                case POSITIVE -> ColorPalette.CAUSAL_POSITIVE;
                case NEGATIVE -> ColorPalette.CAUSAL_NEGATIVE;
                case UNKNOWN -> ColorPalette.CAUSAL_UNKNOWN;
            };

            // Place label at t ≈ 0.8 on the curve, offset perpendicular to the tangent
            double labelT = 0.8;
            double[] labelPt = CausalLinkGeometry.evaluate(fromX, fromY, cp.x(), cp.y(), toX, toY, labelT);
            double[] labelTan = CausalLinkGeometry.tangent(fromX, fromY, cp.x(), cp.y(), toX, toY, labelT);
            double tanDist = Math.sqrt(labelTan[0] * labelTan[0] + labelTan[1] * labelTan[1]);
            if (tanDist > 0) {
                double perpX = -labelTan[1] / tanDist;
                double perpY = labelTan[0] / tanDist;
                double perpOffset = 12;
                double labelX = labelPt[0] + perpX * perpOffset;
                double labelY = labelPt[1] + perpY * perpOffset;

                Font labelFont = polarity == CausalLinkDef.Polarity.UNKNOWN
                        ? LayoutMetrics.CAUSAL_UNKNOWN_FONT : LayoutMetrics.CAUSAL_POLARITY_FONT;
                gc.setFill(labelColor);
                gc.setFont(labelFont);
                gc.setTextAlign(TextAlignment.CENTER);
                gc.setTextBaseline(VPos.CENTER);
                gc.fillText(polarity.symbol(), labelX, labelY);
            }
        }
    }

    /**
     * Draws a self-loop causal link as a cubic Bézier curve.
     */
    public static void drawCausalLinkSelfLoop(GraphicsContext gc,
                                               double[] loopPts,
                                               CausalLinkDef.Polarity polarity) {
        double startX = loopPts[0];
        double startY = loopPts[1];
        double cp1X = loopPts[2];
        double cp1Y = loopPts[3];
        double cp2X = loopPts[4];
        double cp2Y = loopPts[5];
        double endX = loopPts[6];
        double endY = loopPts[7];

        // Draw curve
        Color linkColor = polarity == CausalLinkDef.Polarity.UNKNOWN
                ? ColorPalette.CAUSAL_UNKNOWN : ColorPalette.CAUSAL_LINK;
        gc.setStroke(linkColor);
        gc.setLineWidth(LayoutMetrics.CAUSAL_LINK_WIDTH);
        if (polarity == CausalLinkDef.Polarity.UNKNOWN) {
            gc.setLineDashes(4, 3);
        } else {
            gc.setLineDashes();
        }

        int segments = 30;
        gc.beginPath();
        gc.moveTo(startX, startY);
        // Stop a bit before the end for the arrowhead
        double stopT = 0.9;
        for (int i = 1; i <= segments; i++) {
            double t = stopT * i / segments;
            double[] pt = CausalLinkGeometry.evaluateCubic(
                    startX, startY, cp1X, cp1Y, cp2X, cp2Y, endX, endY, t);
            gc.lineTo(pt[0], pt[1]);
        }
        gc.stroke();
        gc.setLineDashes();

        // Arrowhead at the end
        double[] tan = CausalLinkGeometry.tangentCubic(
                startX, startY, cp1X, cp1Y, cp2X, cp2Y, endX, endY, 1.0);
        drawArrowheadFromTangent(gc, endX, endY, tan[0], tan[1],
                LayoutMetrics.CAUSAL_ARROWHEAD_LENGTH, LayoutMetrics.CAUSAL_ARROWHEAD_WIDTH,
                linkColor);

        // Polarity label at the top of the loop
        double[] midPt = CausalLinkGeometry.evaluateCubic(
                startX, startY, cp1X, cp1Y, cp2X, cp2Y, endX, endY, 0.5);
        Color labelColor = switch (polarity) {
            case POSITIVE -> ColorPalette.CAUSAL_POSITIVE;
            case NEGATIVE -> ColorPalette.CAUSAL_NEGATIVE;
            case UNKNOWN -> ColorPalette.CAUSAL_UNKNOWN;
        };
        Font labelFont = polarity == CausalLinkDef.Polarity.UNKNOWN
                ? LayoutMetrics.CAUSAL_UNKNOWN_FONT : LayoutMetrics.CAUSAL_POLARITY_FONT;
        gc.setFill(labelColor);
        gc.setFont(labelFont);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.fillText(polarity.symbol(), midPt[0], midPt[1] - 10);
    }

    /**
     * Draws a filled arrowhead at (tipX,tipY) oriented along the given tangent vector.
     */
    private static void drawArrowheadFromTangent(GraphicsContext gc,
                                                  double tipX, double tipY,
                                                  double tanX, double tanY,
                                                  double length, double width,
                                                  javafx.scene.paint.Color color) {
        double dist = Math.sqrt(tanX * tanX + tanY * tanY);
        if (dist < 1e-9) {
            return;
        }

        double ux = tanX / dist;
        double uy = tanY / dist;

        double baseX = tipX - ux * length;
        double baseY = tipY - uy * length;

        double perpX = -uy * width / 2;
        double perpY = ux * width / 2;

        double[] xPoints = {tipX, baseX + perpX, baseX - perpX};
        double[] yPoints = {tipY, baseY + perpY, baseY - perpY};

        gc.setFill(color);
        gc.fillPolygon(xPoints, yPoints, 3);
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

        // Draw a "~" inside to distinguish from a plain circle
        gc.setFill(ColorPalette.CLOUD);
        gc.setFont(LayoutMetrics.CLOUD_SYMBOL_FONT);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.fillText("~", cx, cy);
    }
}

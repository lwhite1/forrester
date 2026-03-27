package systems.courant.sd.app.canvas.renderers;

import systems.courant.sd.model.def.CausalLinkDef;

import javafx.geometry.VPos;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import systems.courant.sd.app.canvas.ArrowheadGeometry;
import systems.courant.sd.app.canvas.CausalLinkGeometry;
import systems.courant.sd.app.canvas.ColorPalette;
import systems.courant.sd.app.canvas.FlowEndpointCalculator;
import systems.courant.sd.app.canvas.LayoutMetrics;
import systems.courant.sd.app.canvas.PolarityLabelLayout;

/**
 * Static methods for drawing material flows and info links between elements.
 * Geometry computations are delegated to {@link ArrowheadGeometry} and
 * {@link PolarityLabelLayout} so they can be shared with the SVG exporter.
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
     * The path is: source -> diamond -> sink, with clouds drawn at endpoints marked as clouds.
     * All coordinates must be concrete (no NaN) -- the caller is responsible for computing
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

        // Source -> diamond segment (no arrowhead -- draw full length)
        gc.setStroke(color);
        gc.setLineWidth(LayoutMetrics.MATERIAL_FLOW_WIDTH);
        gc.setLineDashes();
        gc.strokeLine(pipeSourceX, pipeSourceY, midX, midY);

        // Diamond -> sink segment: stop line at arrowhead base
        double[] stop = ArrowheadGeometry.lineStopPoint(
                midX, midY, pipeSinkX, pipeSinkY, LayoutMetrics.ARROWHEAD_LENGTH);
        gc.strokeLine(midX, midY, stop[0], stop[1]);

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
        double[] stop = ArrowheadGeometry.lineStopPoint(
                fromX, fromY, toX, toY, LayoutMetrics.INFO_ARROWHEAD_LENGTH);

        gc.setStroke(ColorPalette.INFO_LINK);
        gc.setLineWidth(LayoutMetrics.INFO_LINK_WIDTH);
        gc.setLineDashes(LayoutMetrics.INFO_LINK_DASH_LENGTH, LayoutMetrics.INFO_LINK_DASH_GAP);
        gc.strokeLine(fromX, fromY, stop[0], stop[1]);
        gc.setLineDashes();

        drawArrowhead(gc, fromX, fromY, toX, toY,
                LayoutMetrics.INFO_ARROWHEAD_LENGTH, LayoutMetrics.INFO_ARROWHEAD_WIDTH,
                ColorPalette.INFO_LINK);
    }

    /**
     * Draws a causal link as a quadratic Bezier curve with arrowhead and polarity label.
     */
    public static void drawCausalLink(GraphicsContext gc,
                                      double fromX, double fromY,
                                      double toX, double toY,
                                      CausalLinkGeometry.ControlPoint cp,
                                      CausalLinkDef.Polarity polarity) {
        drawCausalLink(gc, fromX, fromY, toX, toY, cp, polarity, null);
    }

    /**
     * Draws a causal link with an optional custom color override.
     */
    public static void drawCausalLink(GraphicsContext gc,
                                      double fromX, double fromY,
                                      double toX, double toY,
                                      CausalLinkGeometry.ControlPoint cp,
                                      CausalLinkDef.Polarity polarity,
                                      Color customColor) {
        // Compute arrowhead placement along the curve
        double[] ah = CausalLinkGeometry.arrowheadPoint(fromX, fromY, cp.x(), cp.y(),
                toX, toY, LayoutMetrics.CAUSAL_ARROWHEAD_LENGTH);
        double tipX = ah[0];
        double tipY = ah[1];
        double tanX = ah[2];
        double tanY = ah[3];
        double stopT = ah[4];

        // Draw the curved line, stopping at the arrowhead base
        Color linkColor;
        if (customColor != null) {
            linkColor = customColor;
        } else {
            linkColor = polarity == CausalLinkDef.Polarity.UNKNOWN
                    ? ColorPalette.CAUSAL_UNKNOWN : ColorPalette.CAUSAL_LINK;
        }
        gc.setStroke(linkColor);
        gc.setLineWidth(LayoutMetrics.CAUSAL_LINK_WIDTH);
        if (customColor == null && polarity == CausalLinkDef.Polarity.UNKNOWN) {
            gc.setLineDashes(4, 3);
        } else {
            gc.setLineDashes();
        }

        CausalLinkGeometry.strokeQuadCurve(gc, fromX, fromY, cp.x(), cp.y(), toX, toY, stopT);
        gc.setLineDashes();

        // Arrowhead oriented along the curve tangent at the tip
        ArrowheadGeometry arrowhead = ArrowheadGeometry.fromTangent(tipX, tipY, tanX, tanY,
                LayoutMetrics.CAUSAL_ARROWHEAD_LENGTH, LayoutMetrics.CAUSAL_ARROWHEAD_WIDTH);
        if (arrowhead != null) {
            fillArrowhead(gc, arrowhead, linkColor);
        }

        // Polarity label near the arrowhead, offset perpendicular to the curve tangent
        PolarityLabelLayout labelLayout = PolarityLabelLayout.forQuadratic(
                fromX, fromY, cp.x(), cp.y(), toX, toY);
        if (labelLayout.valid()) {
            Color labelColor = customColor != null ? customColor : PolarityLabelLayout.colorFor(polarity);
            Font labelFont = polarity == CausalLinkDef.Polarity.UNKNOWN
                    ? LayoutMetrics.CAUSAL_UNKNOWN_FONT : LayoutMetrics.CAUSAL_POLARITY_FONT;
            gc.setFill(labelColor);
            gc.setFont(labelFont);
            gc.setTextAlign(TextAlignment.CENTER);
            gc.setTextBaseline(VPos.CENTER);
            gc.fillText(polarity.symbol(), labelLayout.x(), labelLayout.y());
        }
    }

    /**
     * Draws a self-loop causal link as a cubic Bezier curve.
     */
    public static void drawCausalLinkSelfLoop(GraphicsContext gc,
                                               double[] loopPts,
                                               CausalLinkDef.Polarity polarity) {
        drawCausalLinkSelfLoop(gc, loopPts, polarity, null);
    }

    /**
     * Draws a self-loop causal link with an optional custom color override.
     */
    public static void drawCausalLinkSelfLoop(GraphicsContext gc,
                                               double[] loopPts,
                                               CausalLinkDef.Polarity polarity,
                                               Color customColor) {
        double startX = loopPts[0];
        double startY = loopPts[1];
        double cp1X = loopPts[2];
        double cp1Y = loopPts[3];
        double cp2X = loopPts[4];
        double cp2Y = loopPts[5];
        double endX = loopPts[6];
        double endY = loopPts[7];

        // Draw curve
        Color linkColor;
        if (customColor != null) {
            linkColor = customColor;
        } else {
            linkColor = polarity == CausalLinkDef.Polarity.UNKNOWN
                    ? ColorPalette.CAUSAL_UNKNOWN : ColorPalette.CAUSAL_LINK;
        }
        gc.setStroke(linkColor);
        gc.setLineWidth(LayoutMetrics.CAUSAL_LINK_WIDTH);
        if (customColor == null && polarity == CausalLinkDef.Polarity.UNKNOWN) {
            gc.setLineDashes(4, 3);
        } else {
            gc.setLineDashes();
        }

        // Stop a bit before the end for the arrowhead
        double stopT = 0.9;
        CausalLinkGeometry.strokeCubicCurve(gc,
                startX, startY, cp1X, cp1Y, cp2X, cp2Y, endX, endY, stopT);
        gc.setLineDashes();

        // Arrowhead at the end
        double[] tan = CausalLinkGeometry.tangentCubic(
                startX, startY, cp1X, cp1Y, cp2X, cp2Y, endX, endY, 1.0);
        ArrowheadGeometry arrowhead = ArrowheadGeometry.fromTangent(endX, endY, tan[0], tan[1],
                LayoutMetrics.CAUSAL_ARROWHEAD_LENGTH, LayoutMetrics.CAUSAL_ARROWHEAD_WIDTH);
        if (arrowhead != null) {
            fillArrowhead(gc, arrowhead, linkColor);
        }

        // Polarity label at the top of the loop
        PolarityLabelLayout labelLayout = PolarityLabelLayout.forSelfLoop(loopPts);
        Color labelColor = customColor != null ? customColor : PolarityLabelLayout.colorFor(polarity);
        Font labelFont = polarity == CausalLinkDef.Polarity.UNKNOWN
                ? LayoutMetrics.CAUSAL_UNKNOWN_FONT : LayoutMetrics.CAUSAL_POLARITY_FONT;
        gc.setFill(labelColor);
        gc.setFont(labelFont);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.fillText(polarity.symbol(), labelLayout.x(), labelLayout.y());
    }

    /**
     * Fills a pre-computed arrowhead triangle on the graphics context.
     */
    private static void fillArrowhead(GraphicsContext gc, ArrowheadGeometry ah, Color color) {
        double[] xPoints = {ah.tipX(), ah.baseLeftX(), ah.baseRightX()};
        double[] yPoints = {ah.tipY(), ah.baseLeftY(), ah.baseRightY()};
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
                                      Color color) {
        ArrowheadGeometry ah = ArrowheadGeometry.fromLine(fromX, fromY, toX, toY, length, width);
        if (ah != null) {
            fillArrowhead(gc, ah, color);
        }
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

package systems.courant.sd.app.canvas;

import systems.courant.sd.model.def.CausalLinkDef;

/**
 * Computes the position for a causal link polarity label along a Bezier curve.
 * Extracts the perpendicular-offset computation that was duplicated between
 * {@link systems.courant.sd.app.canvas.renderers.ConnectionRenderer} and
 * {@link SvgExporter}.
 *
 * @param x      label center x
 * @param y      label center y
 * @param valid  true if a valid position was computed (false when endpoints are coincident)
 */
public record PolarityLabelLayout(double x, double y, boolean valid) {

    /** Perpendicular offset distance from the curve to the label center. */
    private static final double PERP_OFFSET = 12;

    /** Parameter position along the curve where the label is placed. */
    private static final double LABEL_T = 0.8;

    /**
     * Computes the polarity label position for a quadratic Bezier causal link.
     * The label is placed at t=0.8 along the curve, offset perpendicular to the tangent.
     *
     * @param fromX  curve start x (clipped to element border)
     * @param fromY  curve start y
     * @param cpX    control point x
     * @param cpY    control point y
     * @param toX    curve end x (clipped to element border)
     * @param toY    curve end y
     * @return the label layout; {@code valid} is false if endpoints are too close
     */
    public static PolarityLabelLayout forQuadratic(double fromX, double fromY,
                                                   double cpX, double cpY,
                                                   double toX, double toY) {
        double dx = toX - fromX;
        double dy = toY - fromY;
        double dist = Math.sqrt(dx * dx + dy * dy);
        if (dist <= 1) {
            return new PolarityLabelLayout(0, 0, false);
        }

        double[] labelPt = CausalLinkGeometry.evaluateAsArc(fromX, fromY, cpX, cpY, toX, toY, LABEL_T);
        double[] labelTan = CausalLinkGeometry.tangentAsArc(fromX, fromY, cpX, cpY, toX, toY, LABEL_T);
        double tanDist = Math.sqrt(labelTan[0] * labelTan[0] + labelTan[1] * labelTan[1]);
        if (tanDist <= 0) {
            return new PolarityLabelLayout(0, 0, false);
        }

        double perpX = -labelTan[1] / tanDist;
        double perpY = labelTan[0] / tanDist;
        return new PolarityLabelLayout(
                labelPt[0] + perpX * PERP_OFFSET,
                labelPt[1] + perpY * PERP_OFFSET,
                true);
    }

    /**
     * Computes the polarity label position for a cubic Bezier self-loop.
     * The label is placed at the midpoint of the loop curve, offset above.
     *
     * @param loopPts 8-element array from {@link CausalLinkGeometry#selfLoopPoints}:
     *                {startX, startY, cp1X, cp1Y, cp2X, cp2Y, endX, endY}
     * @return the label layout (always valid for self-loops)
     */
    public static PolarityLabelLayout forSelfLoop(double[] loopPts) {
        double[] midPt = CausalLinkGeometry.evaluateCubic(
                loopPts[0], loopPts[1], loopPts[2], loopPts[3],
                loopPts[4], loopPts[5], loopPts[6], loopPts[7], 0.5);
        return new PolarityLabelLayout(midPt[0], midPt[1] - 10, true);
    }

    /**
     * Returns the label color for a given polarity.
     */
    public static javafx.scene.paint.Color colorFor(CausalLinkDef.Polarity polarity) {
        return switch (polarity) {
            case POSITIVE -> ColorPalette.CAUSAL_POSITIVE;
            case NEGATIVE -> ColorPalette.CAUSAL_NEGATIVE;
            case UNKNOWN -> ColorPalette.CAUSAL_UNKNOWN;
        };
    }
}

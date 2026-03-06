package com.deathrayresearch.forrester.app.canvas;

import com.deathrayresearch.forrester.model.def.CausalLinkDef;

import java.util.List;

/**
 * Geometry utilities for curved causal links using quadratic Bézier curves.
 * Computes control points, samples curves for hit-testing, and provides
 * tangent vectors for arrowhead orientation.
 */
public final class CausalLinkGeometry {

    /** Default perpendicular offset for the curve control point. */
    static final double DEFAULT_BULGE = 40;

    /** Radius for self-loop arcs. */
    static final double SELF_LOOP_RADIUS = 50;

    /** Number of sample segments for hit-testing a curve. */
    private static final int HIT_TEST_SAMPLES = 20;

    private CausalLinkGeometry() {
    }

    /**
     * A quadratic Bézier control point for a causal link curve.
     */
    public record ControlPoint(double x, double y) {
    }

    /**
     * Computes the control point for a causal link curve.
     * <p>
     * The control point is offset perpendicular to the chord (from→to) at its midpoint.
     * When a reciprocal link (to→from) exists, both links curve in opposite directions
     * so they don't overlap.
     * <p>
     * Self-loops (from == to) produce a control point above the element.
     */
    public static ControlPoint controlPoint(double fromX, double fromY,
                                             double toX, double toY,
                                             String fromName, String toName,
                                             List<CausalLinkDef> allLinks) {
        // Self-loop: control point offset above the element
        if (fromName.equals(toName)) {
            return new ControlPoint(fromX, fromY - SELF_LOOP_RADIUS * 2);
        }

        double midX = (fromX + toX) / 2;
        double midY = (fromY + toY) / 2;

        double dx = toX - fromX;
        double dy = toY - fromY;
        double dist = Math.sqrt(dx * dx + dy * dy);

        if (dist < 1) {
            return new ControlPoint(midX, midY);
        }

        // Perpendicular unit vector (rotated 90° counter-clockwise)
        double perpX = -dy / dist;
        double perpY = dx / dist;

        // Scale bulge with distance — enough curve to be visible but not excessive
        double bulge = Math.min(DEFAULT_BULGE, dist * 0.25);

        // Check for reciprocal link (to→from). If it exists, this link curves one way
        // and the reciprocal curves the other way.
        int direction = curveDirection(fromName, toName, allLinks);

        return new ControlPoint(
                midX + perpX * bulge * direction,
                midY + perpY * bulge * direction
        );
    }

    /**
     * Returns +1 or -1 to determine which side of the chord this link should curve toward.
     * When reciprocal links exist, the lexicographically-first "from" curves left (+1)
     * and the other curves right (-1). Without reciprocals, always curves left (+1).
     */
    private static int curveDirection(String fromName, String toName,
                                       List<CausalLinkDef> allLinks) {
        boolean hasReciprocal = false;
        for (CausalLinkDef link : allLinks) {
            if (link.from().equals(toName) && link.to().equals(fromName)) {
                hasReciprocal = true;
                break;
            }
        }

        if (!hasReciprocal) {
            return 1;
        }

        // Consistent direction: lexicographic comparison decides which side
        return fromName.compareTo(toName) < 0 ? 1 : -1;
    }

    /**
     * Evaluates a point on the quadratic Bézier curve at parameter t ∈ [0,1].
     * B(t) = (1-t)²·P0 + 2(1-t)t·CP + t²·P1
     */
    public static double[] evaluate(double fromX, double fromY,
                                     double cpX, double cpY,
                                     double toX, double toY, double t) {
        double u = 1 - t;
        double x = u * u * fromX + 2 * u * t * cpX + t * t * toX;
        double y = u * u * fromY + 2 * u * t * cpY + t * t * toY;
        return new double[]{x, y};
    }

    /**
     * Returns the tangent vector (unnormalized) at parameter t on the quadratic Bézier.
     * B'(t) = 2(1-t)(CP-P0) + 2t(P1-CP)
     */
    public static double[] tangent(double fromX, double fromY,
                                    double cpX, double cpY,
                                    double toX, double toY, double t) {
        double u = 1 - t;
        double tx = 2 * u * (cpX - fromX) + 2 * t * (toX - cpX);
        double ty = 2 * u * (cpY - fromY) + 2 * t * (toY - cpY);
        return new double[]{tx, ty};
    }

    /**
     * Computes the minimum distance from a point to the quadratic Bézier curve
     * by sampling the curve at discrete intervals.
     */
    public static double pointToCurveDistance(double px, double py,
                                              double fromX, double fromY,
                                              double cpX, double cpY,
                                              double toX, double toY) {
        double minDist = Double.MAX_VALUE;

        for (int i = 0; i <= HIT_TEST_SAMPLES; i++) {
            double t = (double) i / HIT_TEST_SAMPLES;
            double[] pt = evaluate(fromX, fromY, cpX, cpY, toX, toY, t);
            double dx = px - pt[0];
            double dy = py - pt[1];
            double dist = Math.sqrt(dx * dx + dy * dy);
            if (dist < minDist) {
                minDist = dist;
            }
        }

        return minDist;
    }

    /**
     * Computes the point on the curve where the arrowhead tip should be placed,
     * backing off from t=1 by the arrowhead length along the curve.
     * Returns {x, y, tangentX, tangentY} for positioning and orienting the arrowhead.
     */
    public static double[] arrowheadPoint(double fromX, double fromY,
                                           double cpX, double cpY,
                                           double toX, double toY,
                                           double arrowLength) {
        // Walk backward from t=1 to find the point at arrowLength distance from the tip
        double[] tip = evaluate(fromX, fromY, cpX, cpY, toX, toY, 1.0);
        double bestT = 1.0;

        for (int i = HIT_TEST_SAMPLES - 1; i >= 0; i--) {
            double t = (double) i / HIT_TEST_SAMPLES;
            double[] pt = evaluate(fromX, fromY, cpX, cpY, toX, toY, t);
            double dx = tip[0] - pt[0];
            double dy = tip[1] - pt[1];
            double dist = Math.sqrt(dx * dx + dy * dy);
            if (dist >= arrowLength) {
                bestT = t;
                break;
            }
        }

        double[] tan = tangent(fromX, fromY, cpX, cpY, toX, toY, 1.0);
        return new double[]{tip[0], tip[1], tan[0], tan[1], bestT};
    }

    /**
     * Self-loop geometry: returns the two endpoints and control points for
     * a cubic Bézier that forms a loop from an element back to itself.
     * The loop goes up and to the right of the element center.
     *
     * @return {startX, startY, cp1X, cp1Y, cp2X, cp2Y, endX, endY}
     */
    public static double[] selfLoopPoints(double cx, double cy, double halfW, double halfH) {
        // Start from the top-right, loop above, end at the top-left
        double startX = cx + halfW * 0.5;
        double startY = cy - halfH;
        double endX = cx - halfW * 0.5;
        double endY = cy - halfH;

        double r = SELF_LOOP_RADIUS;
        double cp1X = startX + r * 0.8;
        double cp1Y = startY - r * 1.5;
        double cp2X = endX - r * 0.8;
        double cp2Y = endY - r * 1.5;

        return new double[]{startX, startY, cp1X, cp1Y, cp2X, cp2Y, endX, endY};
    }

    /**
     * Evaluates a point on a cubic Bézier at parameter t.
     */
    public static double[] evaluateCubic(double p0x, double p0y,
                                          double cp1x, double cp1y,
                                          double cp2x, double cp2y,
                                          double p1x, double p1y, double t) {
        double u = 1 - t;
        double x = u * u * u * p0x + 3 * u * u * t * cp1x + 3 * u * t * t * cp2x + t * t * t * p1x;
        double y = u * u * u * p0y + 3 * u * u * t * cp1y + 3 * u * t * t * cp2y + t * t * t * p1y;
        return new double[]{x, y};
    }

    /**
     * Tangent of a cubic Bézier at parameter t.
     */
    public static double[] tangentCubic(double p0x, double p0y,
                                         double cp1x, double cp1y,
                                         double cp2x, double cp2y,
                                         double p1x, double p1y, double t) {
        double u = 1 - t;
        double tx = 3 * u * u * (cp1x - p0x) + 6 * u * t * (cp2x - cp1x) + 3 * t * t * (p1x - cp2x);
        double ty = 3 * u * u * (cp1y - p0y) + 6 * u * t * (cp2y - cp1y) + 3 * t * t * (p1y - cp2y);
        return new double[]{tx, ty};
    }

    /**
     * Point-to-cubic-Bézier distance by sampling.
     */
    public static double pointToCubicDistance(double px, double py,
                                              double p0x, double p0y,
                                              double cp1x, double cp1y,
                                              double cp2x, double cp2y,
                                              double p1x, double p1y) {
        double minDist = Double.MAX_VALUE;
        for (int i = 0; i <= HIT_TEST_SAMPLES; i++) {
            double t = (double) i / HIT_TEST_SAMPLES;
            double[] pt = evaluateCubic(p0x, p0y, cp1x, cp1y, cp2x, cp2y, p1x, p1y, t);
            double dx = px - pt[0];
            double dy = py - pt[1];
            double dist = Math.sqrt(dx * dx + dy * dy);
            if (dist < minDist) {
                minDist = dist;
            }
        }
        return minDist;
    }
}

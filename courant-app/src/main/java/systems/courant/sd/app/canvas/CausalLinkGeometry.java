package systems.courant.sd.app.canvas;

import systems.courant.sd.model.def.CausalLinkDef;
import systems.courant.sd.model.def.CldVariableDef;
import systems.courant.sd.model.def.ElementType;
import systems.courant.sd.model.graph.CldLoopInfo;

import javafx.scene.canvas.GraphicsContext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Geometry utilities for curved causal links using quadratic Bézier curves.
 * Computes control points, samples curves for hit-testing, and provides
 * tangent vectors for arrowhead orientation.
 */
public final class CausalLinkGeometry {

    /** Default perpendicular offset for the curve control point. */
    public static final double DEFAULT_BULGE = 40;

    /** Radius for self-loop arcs. */
    public static final double SELF_LOOP_RADIUS = 50;

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

        // Use a canonical perpendicular direction based on the lexicographically
        // smaller name, so that A→B and B→A compute the same perpendicular axis.
        // This prevents the double-negation bug where swapping from/to negates
        // both the perpendicular and the direction, causing them to cancel out.
        boolean canonical = fromName.compareTo(toName) < 0;
        double cdx = canonical ? dx : -dx;
        double cdy = canonical ? dy : -dy;

        // Perpendicular unit vector (rotated 90° counter-clockwise from canonical direction)
        double perpX = -cdy / dist;
        double perpY = cdx / dist;

        // Scale bulge with distance — enough curve to be visible but not excessive
        double bulge = Math.min(DEFAULT_BULGE, dist * 0.25);

        // Apply user-defined strength override if present
        double strength = findStrength(fromName, toName, allLinks);
        if (!Double.isNaN(strength)) {
            bulge = strength;
        }

        // Check for reciprocal link (to→from). If it exists, this link curves one way
        // and the reciprocal curves the other way.
        int direction = curveDirection(fromName, toName, allLinks);

        return new ControlPoint(
                midX + perpX * bulge * direction,
                midY + perpY * bulge * direction
        );
    }

    /**
     * Finds the strength override for a specific causal link, or NaN if none.
     */
    static double findStrength(String fromName, String toName,
                                       List<CausalLinkDef> allLinks) {
        for (CausalLinkDef link : allLinks) {
            if (link.from().equals(fromName) && link.to().equals(toName)) {
                return link.strength();
            }
        }
        return Double.NaN;
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
     * Centroid-aware control point. Blends the perpendicular offset direction
     * with an outward direction from the graph centroid, producing curves that
     * bow away from the center of the diagram.
     *
     * @param centroidX X-coordinate of the graph centroid (or NaN to fall back)
     * @param centroidY Y-coordinate of the graph centroid (or NaN to fall back)
     */
    public static ControlPoint controlPoint(double fromX, double fromY,
                                             double toX, double toY,
                                             String fromName, String toName,
                                             List<CausalLinkDef> allLinks,
                                             double centroidX, double centroidY) {
        if (Double.isNaN(centroidX) || Double.isNaN(centroidY)) {
            return controlPoint(fromX, fromY, toX, toY, fromName, toName, allLinks);
        }

        // Self-loop: delegate to centroid-aware self-loop
        if (fromName.equals(toName)) {
            return new ControlPoint(fromX, fromY - SELF_LOOP_RADIUS * 2);
        }

        double midX = (fromX + toX) / 2;
        double midY = (fromY + toY) / 2;

        double dx = toX - fromX;
        double dy = toY - fromY;
        double chordLen = Math.sqrt(dx * dx + dy * dy);

        if (chordLen < 1) {
            return new ControlPoint(midX, midY);
        }

        // d_perp: perpendicular to chord (canonical direction)
        boolean canonical = fromName.compareTo(toName) < 0;
        double cdx = canonical ? dx : -dx;
        double cdy = canonical ? dy : -dy;
        double perpX = -cdy / chordLen;
        double perpY = cdx / chordLen;

        // d_out: direction from centroid through midpoint
        double outDx = midX - centroidX;
        double outDy = midY - centroidY;
        double outDist = Math.sqrt(outDx * outDx + outDy * outDy);

        double finalDirX;
        double finalDirY;

        if (outDist < 1) {
            // M ≈ G — fall back to pure perpendicular
            finalDirX = perpX;
            finalDirY = perpY;
        } else {
            double outNx = outDx / outDist;
            double outNy = outDy / outDist;

            // Flip perpendicular if it points toward the centroid so the
            // curve always tends to bow outward
            double dot = outNx * perpX + outNy * perpY;
            if (dot < 0) {
                perpX = -perpX;
                perpY = -perpY;
                dot = -dot;
            }

            // w = clamp(dot, 0, 1)
            double w = Math.min(1, dot);

            // d_final = normalize(lerp(d_perp, d_out, w))
            finalDirX = perpX * (1 - w) + outNx * w;
            finalDirY = perpY * (1 - w) + outNy * w;
            double finalLen = Math.sqrt(finalDirX * finalDirX + finalDirY * finalDirY);
            if (finalLen > 0.001) {
                finalDirX /= finalLen;
                finalDirY /= finalLen;
            }
        }

        double bulge = 0.35 * chordLen;
        // Cap the bulge to avoid excessively wide curves
        bulge = Math.min(bulge, 120);

        // Apply user-defined strength override if present
        double strength = findStrength(fromName, toName, allLinks);
        if (!Double.isNaN(strength)) {
            bulge = strength;
        }

        int direction = curveDirection(fromName, toName, allLinks);

        return new ControlPoint(
                midX + finalDirX * bulge * direction,
                midY + finalDirY * bulge * direction
        );
    }

    /**
     * Centroid-aware self-loop geometry. The loop bulges radially outward
     * from the centroid.
     *
     * @return {startX, startY, cp1X, cp1Y, cp2X, cp2Y, endX, endY}
     */
    public static double[] selfLoopPoints(double cx, double cy,
                                           double halfW, double halfH,
                                           double centroidX, double centroidY) {
        if (Double.isNaN(centroidX) || Double.isNaN(centroidY)) {
            return selfLoopPoints(cx, cy, halfW, halfH);
        }

        // Direction from centroid to node center
        double dx = cx - centroidX;
        double dy = cy - centroidY;
        double dist = Math.sqrt(dx * dx + dy * dy);

        if (dist < 1) {
            // Node is at centroid — default to upward
            return selfLoopPoints(cx, cy, halfW, halfH);
        }

        double nx = dx / dist;
        double ny = dy / dist;

        // Perpendicular for start/end spread
        double px = -ny;
        double py = nx;

        double r = SELF_LOOP_RADIUS * 1.5;
        double spread = Math.max(halfW, halfH) * 0.5;

        double startX = cx + px * spread;
        double startY = cy + py * spread;
        double endX = cx - px * spread;
        double endY = cy - py * spread;

        double cp1X = startX + nx * r + px * r * 0.4;
        double cp1Y = startY + ny * r + py * r * 0.4;
        double cp2X = endX + nx * r - px * r * 0.4;
        double cp2Y = endY + ny * r - py * r * 0.4;

        return new double[]{startX, startY, cp1X, cp1Y, cp2X, cp2Y, endX, endY};
    }

    /**
     * Computes the centroid (mean position) of all CLD variable nodes.
     *
     * @return {centroidX, centroidY}, or null if there are no CLD nodes
     */
    public static double[] graphCentroid(CanvasState canvasState,
                                          List<CldVariableDef> cldVariables) {
        if (cldVariables == null || cldVariables.isEmpty()) {
            return null;
        }
        double sumX = 0, sumY = 0;
        int count = 0;
        for (CldVariableDef v : cldVariables) {
            double x = canvasState.getX(v.name());
            double y = canvasState.getY(v.name());
            if (!Double.isNaN(x) && !Double.isNaN(y)) {
                sumX += x;
                sumY += y;
                count++;
            }
        }
        if (count == 0) {
            return null;
        }
        return new double[]{sumX / count, sumY / count};
    }

    /**
     * Pre-computed loop context for edge routing. Holds the loop membership
     * info, per-loop centroids, and the global centroid so that each edge
     * can be routed using the appropriate reference point.
     */
    public record LoopContext(
            CldLoopInfo loopInfo,
            Map<Integer, double[]> loopCentroids,
            double globalCentroidX,
            double globalCentroidY
    ) {
        /**
         * Returns the centroid to use for an edge between the two named nodes.
         * Always uses the global centroid for consistency — using per-loop
         * centroids causes visual instability when only some nodes are in loops.
         */
        public double[] centroidFor(String fromName, String toName) {
            return new double[]{globalCentroidX, globalCentroidY};
        }

        /**
         * Returns the bulge factor (k) for an edge between the two named nodes.
         * Same-loop: 0.45, cross-loop: 0.25, default: 0.35.
         */
        public double bulgeFactorFor(String fromName, String toName) {
            if (loopInfo == null || loopInfo.isEmpty()) {
                return 0.35;
            }
            if (loopInfo.isSharedNode(fromName) || loopInfo.isSharedNode(toName)) {
                return 0.25;
            }
            if (loopInfo.commonLoopIndex(fromName, toName) >= 0) {
                return 0.45;
            }
            if (!loopInfo.loopsOf(fromName).isEmpty()
                    && !loopInfo.loopsOf(toName).isEmpty()) {
                return 0.25;
            }
            return 0.35;
        }

        /**
         * Returns the centroid to use for a self-loop on the named node.
         * Uses global centroid for consistency with regular edges.
         */
        public double[] selfLoopCentroid(String name) {
            return new double[]{globalCentroidX, globalCentroidY};
        }
    }

    /**
     * Creates a LoopContext from the canvas state and CLD variable list.
     */
    public static LoopContext loopContext(CanvasState state,
                                          List<CldVariableDef> cldVariables) {
        CldLoopInfo loopInfo = state.getCldLoopInfo();
        double[] gc = graphCentroid(state, cldVariables);
        double gx = gc != null ? gc[0] : Double.NaN;
        double gy = gc != null ? gc[1] : Double.NaN;
        Map<Integer, double[]> loopCentroids = computeLoopCentroids(loopInfo, state);
        return new LoopContext(loopInfo, loopCentroids, gx, gy);
    }

    /**
     * Creates a LoopContext from canvas state alone, computing the global
     * centroid from CLD_VARIABLE elements on the canvas.
     */
    public static LoopContext loopContext(CanvasState state) {
        CldLoopInfo loopInfo = state.getCldLoopInfo();
        double sx = 0, sy = 0;
        int count = 0;
        for (String name : state.getDrawOrder()) {
            if (state.getType(name).orElse(null) == ElementType.CLD_VARIABLE) {
                double x = state.getX(name);
                double y = state.getY(name);
                if (!Double.isNaN(x) && !Double.isNaN(y)) {
                    sx += x;
                    sy += y;
                    count++;
                }
            }
        }
        double gx = count > 0 ? sx / count : Double.NaN;
        double gy = count > 0 ? sy / count : Double.NaN;
        Map<Integer, double[]> loopCentroids = computeLoopCentroids(loopInfo, state);
        return new LoopContext(loopInfo, loopCentroids, gx, gy);
    }

    private static Map<Integer, double[]> computeLoopCentroids(CldLoopInfo loopInfo,
                                                                CanvasState state) {
        Map<Integer, double[]> result = new LinkedHashMap<>();
        if (loopInfo == null || loopInfo.isEmpty()) {
            return result;
        }
        for (int i = 0; i < loopInfo.loops().size(); i++) {
            double lsx = 0, lsy = 0;
            int lcnt = 0;
            for (String node : loopInfo.loops().get(i)) {
                double x = state.getX(node);
                double y = state.getY(node);
                if (!Double.isNaN(x) && !Double.isNaN(y)) {
                    lsx += x;
                    lsy += y;
                    lcnt++;
                }
            }
            if (lcnt > 0) {
                result.put(i, new double[]{lsx / lcnt, lsy / lcnt});
            }
        }
        return result;
    }

    /**
     * Loop-aware control point. Uses per-loop centroids for intra-loop edges
     * (k=0.6) and the global centroid for cross-loop edges (k=0.25).
     */
    public static ControlPoint controlPoint(double fromX, double fromY,
                                             double toX, double toY,
                                             String fromName, String toName,
                                             List<CausalLinkDef> allLinks,
                                             LoopContext loopCtx) {
        if (loopCtx == null || Double.isNaN(loopCtx.globalCentroidX())) {
            return controlPoint(fromX, fromY, toX, toY, fromName, toName, allLinks);
        }

        if (fromName.equals(toName)) {
            return new ControlPoint(fromX, fromY - SELF_LOOP_RADIUS * 2);
        }

        double midX = (fromX + toX) / 2;
        double midY = (fromY + toY) / 2;

        double dx = toX - fromX;
        double dy = toY - fromY;
        double chordLen = Math.sqrt(dx * dx + dy * dy);

        if (chordLen < 1) {
            return new ControlPoint(midX, midY);
        }

        boolean canonical = fromName.compareTo(toName) < 0;
        double cdx = canonical ? dx : -dx;
        double cdy = canonical ? dy : -dy;
        double perpX = -cdy / chordLen;
        double perpY = cdx / chordLen;

        double[] centroid = loopCtx.centroidFor(fromName, toName);
        double k = loopCtx.bulgeFactorFor(fromName, toName);

        double outDx = midX - centroid[0];
        double outDy = midY - centroid[1];
        double outDist = Math.sqrt(outDx * outDx + outDy * outDy);

        double finalDirX;
        double finalDirY;

        if (outDist < 1) {
            finalDirX = perpX;
            finalDirY = perpY;
        } else {
            double outNx = outDx / outDist;
            double outNy = outDy / outDist;

            double dot = outNx * perpX + outNy * perpY;
            if (dot < 0) {
                perpX = -perpX;
                perpY = -perpY;
                dot = -dot;
            }

            double w = Math.min(1, dot);
            finalDirX = perpX * (1 - w) + outNx * w;
            finalDirY = perpY * (1 - w) + outNy * w;
            double finalLen = Math.sqrt(finalDirX * finalDirX + finalDirY * finalDirY);
            if (finalLen > 0.001) {
                finalDirX /= finalLen;
                finalDirY /= finalLen;
            }
        }

        double bulge = k * chordLen;
        bulge = Math.min(bulge, 120);

        // Apply user-defined strength override if present
        double strength = findStrength(fromName, toName, allLinks);
        if (!Double.isNaN(strength)) {
            bulge = strength;
        }

        int direction = curveDirection(fromName, toName, allLinks);

        return new ControlPoint(
                midX + finalDirX * bulge * direction,
                midY + finalDirY * bulge * direction
        );
    }

    /**
     * Loop-aware self-loop geometry. Uses the loop-specific centroid for
     * radial direction and 1.5 × node radius for offset distance.
     */
    public static double[] selfLoopPoints(double cx, double cy,
                                           double halfW, double halfH,
                                           LoopContext loopCtx, String nodeName) {
        if (loopCtx == null) {
            return selfLoopPoints(cx, cy, halfW, halfH);
        }
        double[] centroid = loopCtx.selfLoopCentroid(nodeName);
        return selfLoopPoints(cx, cy, halfW, halfH, centroid[0], centroid[1]);
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

    private static final int CURVE_SEGMENTS = 30;

    /**
     * Strokes a quadratic Bézier curve on the graphics context using line segments.
     *
     * @param stopT parameter value to stop at (1.0 for full curve, &lt;1.0 to leave room for arrowhead)
     */
    public static void strokeQuadCurve(GraphicsContext gc,
                                        double fromX, double fromY,
                                        double cpX, double cpY,
                                        double toX, double toY, double stopT) {
        gc.beginPath();
        gc.moveTo(fromX, fromY);
        for (int i = 1; i <= CURVE_SEGMENTS; i++) {
            double t = stopT * i / CURVE_SEGMENTS;
            double[] pt = evaluate(fromX, fromY, cpX, cpY, toX, toY, t);
            gc.lineTo(pt[0], pt[1]);
        }
        gc.stroke();
    }

    /**
     * Strokes a cubic Bézier curve on the graphics context using line segments.
     *
     * @param stopT parameter value to stop at (1.0 for full curve, &lt;1.0 to leave room for arrowhead)
     */
    public static void strokeCubicCurve(GraphicsContext gc,
                                         double p0x, double p0y,
                                         double cp1x, double cp1y,
                                         double cp2x, double cp2y,
                                         double p1x, double p1y, double stopT) {
        gc.beginPath();
        gc.moveTo(p0x, p0y);
        for (int i = 1; i <= CURVE_SEGMENTS; i++) {
            double t = stopT * i / CURVE_SEGMENTS;
            double[] pt = evaluateCubic(p0x, p0y, cp1x, cp1y, cp2x, cp2y, p1x, p1y, t);
            gc.lineTo(pt[0], pt[1]);
        }
        gc.stroke();
    }
}

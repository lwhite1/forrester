package systems.courant.sd.app.canvas;

/**
 * Computed arrowhead triangle geometry. Both the Canvas renderer and SVG exporter
 * consume these pre-computed coordinates instead of duplicating the direction-vector
 * and perpendicular-offset math.
 *
 * <p>An arrowhead is a triangle with its tip at ({@code tipX}, {@code tipY}) pointing
 * in the direction from a source toward the tip. The base is perpendicular to the
 * direction vector at a distance of {@code length} behind the tip.
 *
 * @param tipX    x-coordinate of the arrowhead tip (the point)
 * @param tipY    y-coordinate of the arrowhead tip
 * @param baseLeftX   x-coordinate of the left base vertex
 * @param baseLeftY   y-coordinate of the left base vertex
 * @param baseRightX  x-coordinate of the right base vertex
 * @param baseRightY  y-coordinate of the right base vertex
 */
public record ArrowheadGeometry(
        double tipX, double tipY,
        double baseLeftX, double baseLeftY,
        double baseRightX, double baseRightY
) {

    /**
     * Computes arrowhead geometry for a straight line from ({@code fromX}, {@code fromY})
     * toward ({@code toX}, {@code toY}), with the tip at the "to" point.
     *
     * @param fromX  line origin x
     * @param fromY  line origin y
     * @param toX    line endpoint x (arrowhead tip)
     * @param toY    line endpoint y (arrowhead tip)
     * @param length arrowhead length (tip to base)
     * @param width  arrowhead width (base spread)
     * @return the arrowhead geometry, or {@code null} if the line is too short (distance &lt; 1)
     */
    public static ArrowheadGeometry fromLine(double fromX, double fromY,
                                             double toX, double toY,
                                             double length, double width) {
        double dx = toX - fromX;
        double dy = toY - fromY;
        double dist = Math.sqrt(dx * dx + dy * dy);
        if (dist < 1) {
            return null;
        }

        double ux = dx / dist;
        double uy = dy / dist;
        return fromUnitVector(toX, toY, ux, uy, length, width);
    }

    /**
     * Computes arrowhead geometry from a tangent vector at the tip point.
     * Used for arrowheads on curved paths (Bezier curves) where the direction
     * is given by the curve tangent rather than a straight line.
     *
     * @param tipX   arrowhead tip x
     * @param tipY   arrowhead tip y
     * @param tanX   tangent vector x component (unnormalized)
     * @param tanY   tangent vector y component (unnormalized)
     * @param length arrowhead length (tip to base)
     * @param width  arrowhead width (base spread)
     * @return the arrowhead geometry, or {@code null} if the tangent is near-zero
     */
    public static ArrowheadGeometry fromTangent(double tipX, double tipY,
                                                double tanX, double tanY,
                                                double length, double width) {
        double dist = Math.sqrt(tanX * tanX + tanY * tanY);
        if (dist < 1e-9) {
            return null;
        }

        double ux = tanX / dist;
        double uy = tanY / dist;
        return fromUnitVector(tipX, tipY, ux, uy, length, width);
    }

    /**
     * Computes the point where a line should stop before reaching an arrowhead,
     * so the line ends at the arrowhead's base rather than extending behind it.
     *
     * <p>Given a line from ({@code fromX}, {@code fromY}) to ({@code toX}, {@code toY}),
     * this returns the point that is {@code arrowLength} before the "to" endpoint,
     * measured along the line direction. If the line is shorter than {@code arrowLength},
     * the "to" point is returned unchanged.
     *
     * @param fromX       line start x
     * @param fromY       line start y
     * @param toX         line end x (where the arrowhead tip sits)
     * @param toY         line end y
     * @param arrowLength arrowhead length to subtract
     * @return a 2-element array {stopX, stopY}
     */
    public static double[] lineStopPoint(double fromX, double fromY,
                                         double toX, double toY,
                                         double arrowLength) {
        double dx = toX - fromX;
        double dy = toY - fromY;
        double dist = Math.sqrt(dx * dx + dy * dy);
        if (dist > arrowLength) {
            double ux = dx / dist;
            double uy = dy / dist;
            return new double[]{toX - ux * arrowLength, toY - uy * arrowLength};
        }
        return new double[]{toX, toY};
    }

    private static ArrowheadGeometry fromUnitVector(double tipX, double tipY,
                                                    double ux, double uy,
                                                    double length, double width) {
        double baseX = tipX - ux * length;
        double baseY = tipY - uy * length;

        double perpX = -uy * width / 2;
        double perpY = ux * width / 2;

        return new ArrowheadGeometry(
                tipX, tipY,
                baseX + perpX, baseY + perpY,
                baseX - perpX, baseY - perpY);
    }
}

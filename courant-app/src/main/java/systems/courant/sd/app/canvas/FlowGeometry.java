package systems.courant.sd.app.canvas;

import systems.courant.sd.model.def.ElementType;
import systems.courant.sd.app.canvas.renderers.CanvasRenderer;

/**
 * Shared geometry utilities for flow and connection rendering/hit-testing.
 * Centralizes coordinate calculations used by both CanvasRenderer and
 * FlowEndpointCalculator to prevent duplication.
 */
public final class FlowGeometry {

    private FlowGeometry() {
    }

    /**
     * An immutable 2D point in world coordinates.
     */
    public record Point2D(double x, double y) {}

    /**
     * Clips a line from the center of a rectangle toward a target point,
     * returning the intersection with the rectangle border.
     */
    public static Point2D clipToBorder(double cx, double cy, double halfW, double halfH,
                                        double targetX, double targetY) {
        double dx = targetX - cx;
        double dy = targetY - cy;
        if (dx == 0 && dy == 0) {
            return new Point2D(cx, cy);
        }

        double scaleX = halfW > 0 ? Math.abs(halfW / dx) : Double.MAX_VALUE;
        double scaleY = halfH > 0 ? Math.abs(halfH / dy) : Double.MAX_VALUE;
        double scale = Math.min(scaleX, scaleY);

        return new Point2D(cx + dx * scale, cy + dy * scale);
    }

    /**
     * Clips a line from the center of a rhombus toward a target point,
     * returning the intersection with the rhombus border.
     * The rhombus boundary satisfies {@code |x/hw| + |y/hh| = 1}.
     */
    public static Point2D clipToRhombus(double cx, double cy, double hw, double hh,
                                         double targetX, double targetY) {
        double dx = targetX - cx;
        double dy = targetY - cy;
        if (dx == 0 && dy == 0) {
            return new Point2D(cx, cy);
        }

        double absDx = Math.abs(dx);
        double absDy = Math.abs(dy);
        double denom = (hw > 0 ? absDx / hw : 0) + (hh > 0 ? absDy / hh : 0);
        if (denom < 1e-9) {
            return new Point2D(cx, cy);
        }

        double scale = 1.0 / denom;
        return new Point2D(cx + dx * scale, cy + dy * scale);
    }

    /**
     * Computes the clipped endpoint where a connection line exits an element's border.
     * Uses rhombus clipping for flow diamonds and rectangular clipping for all other elements.
     */
    public static Point2D clipToElement(CanvasState state, String elementName,
                                         double targetX, double targetY) {
        double cx = state.getX(elementName);
        double cy = state.getY(elementName);
        double halfW = LayoutMetrics.effectiveWidth(state, elementName) / 2;
        double halfH = LayoutMetrics.effectiveHeight(state, elementName) / 2;

        ElementType type = state.getType(elementName).orElse(null);
        if (type == ElementType.FLOW) {
            return clipToRhombus(cx, cy, halfW, halfH, targetX, targetY);
        }
        return clipToBorder(cx, cy, halfW, halfH, targetX, targetY);
    }
}

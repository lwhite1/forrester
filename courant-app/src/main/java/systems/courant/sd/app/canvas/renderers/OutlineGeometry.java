package systems.courant.sd.app.canvas.renderers;

import systems.courant.sd.model.def.ElementType;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import systems.courant.sd.app.canvas.CanvasState;
import systems.courant.sd.app.canvas.LayoutMetrics;

/**
 * Computes element outline geometry (diamond for flows, rectangle for other types)
 * and provides shared drawing routines. Eliminates the copy-pasted outline code
 * that was previously spread across {@link SelectionRenderer}, {@link FeedbackLoopRenderer},
 * {@link ErrorIndicatorRenderer}, {@link CausalTraceRenderer}, and
 * {@link systems.courant.sd.app.canvas.SvgExporter}.
 */
public final class OutlineGeometry {

    private OutlineGeometry() {
    }

    /**
     * The resolved shape of an element outline: either a diamond (4-point polygon)
     * for flows, or an axis-aligned rectangle for everything else.
     */
    public sealed interface Shape {

        double cx();

        double cy();
    }

    /**
     * A diamond (rotated square) outline for {@link ElementType#FLOW} elements.
     * The four vertices are at the cardinal points of the center, offset by {@code half}.
     */
    public record Diamond(double cx, double cy, double half,
                          double[] xPoints, double[] yPoints) implements Shape {
    }

    /**
     * A rectangular outline for non-flow elements.
     */
    public record Rect(double cx, double cy,
                       double x, double y, double w, double h) implements Shape {
    }

    /**
     * Resolves the outline shape for the named element, or {@code null} if the element
     * is missing, has no type, or has NaN coordinates.
     *
     * @param state   canvas state containing element positions and sizes
     * @param name    element name
     * @param padding extra space around the element bounds
     * @return the resolved shape, or null if the element cannot be outlined
     */
    public static Shape resolve(CanvasState state, String name, double padding) {
        ElementType type = state.getType(name).orElse(null);
        double cx = state.getX(name);
        double cy = state.getY(name);

        if (type == null || Double.isNaN(cx) || Double.isNaN(cy)) {
            return null;
        }

        if (type == ElementType.FLOW) {
            double half = LayoutMetrics.FLOW_INDICATOR_SIZE / 2 + padding;
            double[] xPoints = {cx, cx + half, cx, cx - half};
            double[] yPoints = {cy - half, cy, cy + half, cy};
            return new Diamond(cx, cy, half, xPoints, yPoints);
        } else {
            double halfW = LayoutMetrics.effectiveWidth(state, name) / 2 + padding;
            double halfH = LayoutMetrics.effectiveHeight(state, name) / 2 + padding;
            double x = cx - halfW;
            double y = cy - halfH;
            return new Rect(cx, cy, x, y, halfW * 2, halfH * 2);
        }
    }

    /**
     * Draws a filled-and-stroked outline around the named element.
     * Sets fill, stroke, and line width on the graphics context, then draws
     * the appropriate shape (diamond polygon or rectangle).
     *
     * <p>The caller is responsible for setting any dash pattern on {@code gc}
     * before calling this method, and for resetting it afterward if needed.
     *
     * @param gc          graphics context to draw on
     * @param state       canvas state containing element positions and sizes
     * @param name        element name
     * @param padding     extra space around the element bounds
     * @param fillColor   fill color (may be transparent)
     * @param strokeColor stroke color
     * @param lineWidth   stroke line width
     */
    public static void drawElementOutline(GraphicsContext gc, CanvasState state, String name,
                                          double padding, Color fillColor, Color strokeColor,
                                          double lineWidth) {
        Shape shape = resolve(state, name, padding);
        if (shape == null) {
            return;
        }

        gc.setStroke(strokeColor);
        gc.setLineWidth(lineWidth);

        switch (shape) {
            case Diamond d -> {
                gc.setFill(fillColor);
                gc.fillPolygon(d.xPoints(), d.yPoints(), 4);
                gc.strokePolygon(d.xPoints(), d.yPoints(), 4);
            }
            case Rect r -> {
                gc.setFill(fillColor);
                gc.fillRect(r.x(), r.y(), r.w(), r.h());
                gc.strokeRect(r.x(), r.y(), r.w(), r.h());
            }
        }
    }

    /**
     * Strokes (without filling) the outline around the named element.
     * Useful for hover indicators that need only a border, no fill.
     *
     * @param gc          graphics context to draw on
     * @param state       canvas state containing element positions and sizes
     * @param name        element name
     * @param padding     extra space around the element bounds
     * @param strokeColor stroke color
     * @param lineWidth   stroke line width
     */
    public static void strokeElementOutline(GraphicsContext gc, CanvasState state, String name,
                                            double padding, Color strokeColor, double lineWidth) {
        Shape shape = resolve(state, name, padding);
        if (shape == null) {
            return;
        }

        gc.setStroke(strokeColor);
        gc.setLineWidth(lineWidth);

        switch (shape) {
            case Diamond d -> gc.strokePolygon(d.xPoints(), d.yPoints(), 4);
            case Rect r -> gc.strokeRect(r.x(), r.y(), r.w(), r.h());
        }
    }
}

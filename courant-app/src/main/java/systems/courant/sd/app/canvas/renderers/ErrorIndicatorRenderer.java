package systems.courant.sd.app.canvas.renderers;

import systems.courant.sd.model.def.ElementType;
import systems.courant.sd.model.def.ValidationIssue.Severity;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import systems.courant.sd.app.canvas.CanvasState;
import systems.courant.sd.app.canvas.ColorPalette;
import systems.courant.sd.app.canvas.LayoutMetrics;

/**
 * Draws error and warning indicators around canvas elements that have validation issues.
 * Follows the same static-method pattern as {@link FeedbackLoopRenderer}.
 */
public final class ErrorIndicatorRenderer {

    private static final double INDICATOR_PADDING = 5;
    private static final double INDICATOR_LINE_WIDTH = 2.0;

    private ErrorIndicatorRenderer() {
    }

    /**
     * Draws an error or warning indicator around the named element.
     * Errors get a red border/fill, warnings get an amber border/fill.
     */
    public static void drawIndicator(GraphicsContext gc, CanvasState state,
                                     String name, Severity severity) {
        ElementType type = state.getType(name).orElse(null);
        double cx = state.getX(name);
        double cy = state.getY(name);

        if (type == null || Double.isNaN(cx) || Double.isNaN(cy)) {
            return;
        }

        Color border = severity == Severity.ERROR
                ? ColorPalette.ERROR_BORDER : ColorPalette.WARNING_BORDER;
        Color fill = severity == Severity.ERROR
                ? ColorPalette.ERROR_FILL : ColorPalette.WARNING_FILL;

        gc.setStroke(border);
        gc.setLineWidth(INDICATOR_LINE_WIDTH);
        gc.setLineDashes();

        if (type == ElementType.FLOW) {
            double half = LayoutMetrics.FLOW_INDICATOR_SIZE / 2 + INDICATOR_PADDING;
            double[] xPoints = {cx, cx + half, cx, cx - half};
            double[] yPoints = {cy - half, cy, cy + half, cy};

            gc.setFill(fill);
            gc.fillPolygon(xPoints, yPoints, 4);
            gc.strokePolygon(xPoints, yPoints, 4);
        } else {
            double halfW = LayoutMetrics.effectiveWidth(state, name) / 2 + INDICATOR_PADDING;
            double halfH = LayoutMetrics.effectiveHeight(state, name) / 2 + INDICATOR_PADDING;
            double x = cx - halfW;
            double y = cy - halfH;
            double w = halfW * 2;
            double h = halfH * 2;

            gc.setFill(fill);
            gc.fillRect(x, y, w, h);
            gc.strokeRect(x, y, w, h);
        }
    }
}

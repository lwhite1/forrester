package systems.courant.sd.app.canvas.renderers;

import systems.courant.sd.model.def.ValidationIssue.Severity;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import systems.courant.sd.app.canvas.CanvasState;
import systems.courant.sd.app.canvas.ColorPalette;

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
        Color border = severity == Severity.ERROR
                ? ColorPalette.ERROR_BORDER : ColorPalette.WARNING_BORDER;
        Color fill = severity == Severity.ERROR
                ? ColorPalette.ERROR_FILL : ColorPalette.WARNING_FILL;

        gc.setLineDashes();
        OutlineGeometry.drawElementOutline(gc, state, name, INDICATOR_PADDING,
                fill, border, INDICATOR_LINE_WIDTH);
    }
}

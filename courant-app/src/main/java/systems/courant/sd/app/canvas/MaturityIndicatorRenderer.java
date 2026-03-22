package systems.courant.sd.app.canvas;

import systems.courant.sd.model.def.ElementType;

import javafx.geometry.VPos;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.text.TextAlignment;

/**
 * Draws maturity indicators on canvas elements that are not fully specified.
 * These visual cues help guide users toward completing their models without
 * requiring AI assistance.
 *
 * <ul>
 *   <li><b>Amber left-edge accent</b> — on elements missing equations</li>
 *   <li><b>"?" unit badge</b> — on elements without units specified</li>
 * </ul>
 *
 * <p>Unit-mismatch indicators on connection lines are drawn by
 * {@link systems.courant.sd.app.canvas.renderers.ConnectionRenderer ConnectionRenderer}
 * using the color from {@link ColorPalette#UNIT_MISMATCH}.
 */
public final class MaturityIndicatorRenderer {

    /** Width of the amber accent stripe on the left edge of incomplete elements. */
    static final double ACCENT_WIDTH = 4;

    /** Padding between the accent and the element border. */
    static final double ACCENT_INSET = 1;

    private MaturityIndicatorRenderer() {
    }

    /**
     * Draws an amber left-edge accent stripe on an element that is missing its equation.
     * For flow elements (diamonds), draws the accent as a vertical line to the left of the diamond.
     */
    public static void drawMissingEquationAccent(GraphicsContext gc, CanvasState state,
                                                  String name) {
        ElementType type = state.getType(name).orElse(null);
        double cx = state.getX(name);
        double cy = state.getY(name);

        if (type == null || Double.isNaN(cx) || Double.isNaN(cy)) {
            return;
        }

        gc.setFill(ColorPalette.MATURITY_ACCENT);
        gc.setStroke(ColorPalette.MATURITY_ACCENT);
        gc.setLineDashes();

        if (type == ElementType.FLOW) {
            double half = LayoutMetrics.FLOW_INDICATOR_SIZE / 2;
            double accentX = cx - half - ACCENT_INSET - ACCENT_WIDTH;
            double accentY = cy - half;
            gc.fillRoundRect(accentX, accentY, ACCENT_WIDTH, half * 2,
                    ACCENT_WIDTH, ACCENT_WIDTH);
        } else {
            double w = LayoutMetrics.effectiveWidth(state, name);
            double h = LayoutMetrics.effectiveHeight(state, name);
            double x = cx - w / 2;
            double y = cy - h / 2;

            double cornerRadius = cornerRadiusFor(type);
            gc.fillRoundRect(x - ACCENT_INSET, y + ACCENT_INSET,
                    ACCENT_WIDTH, h - ACCENT_INSET * 2,
                    cornerRadius, cornerRadius);
        }
    }

    /**
     * Draws a small "?" badge on elements that have no unit specified.
     * The badge appears at the bottom-right corner of the element.
     */
    public static void drawMissingUnitBadge(GraphicsContext gc, CanvasState state,
                                             String name) {
        ElementType type = state.getType(name).orElse(null);
        double cx = state.getX(name);
        double cy = state.getY(name);

        if (type == null || Double.isNaN(cx) || Double.isNaN(cy)) {
            return;
        }

        double badgeX;
        double badgeY;

        if (type == ElementType.FLOW) {
            double half = LayoutMetrics.FLOW_INDICATOR_SIZE / 2;
            badgeX = cx + half + 2;
            badgeY = cy + half;
        } else {
            double w = LayoutMetrics.effectiveWidth(state, name);
            double h = LayoutMetrics.effectiveHeight(state, name);
            badgeX = cx + w / 2 - 6;
            badgeY = cy + h / 2 - 4;
        }

        gc.setFill(ColorPalette.MATURITY_UNIT_BADGE);
        gc.setFont(LayoutMetrics.BADGE_FONT);
        gc.setTextAlign(TextAlignment.RIGHT);
        gc.setTextBaseline(VPos.BOTTOM);
        gc.fillText("?", badgeX, badgeY);
    }

    private static double cornerRadiusFor(ElementType type) {
        return switch (type) {
            case STOCK -> LayoutMetrics.STOCK_CORNER_RADIUS;
            case AUX -> LayoutMetrics.AUX_CORNER_RADIUS;
            case MODULE -> LayoutMetrics.MODULE_CORNER_RADIUS;
            case LOOKUP -> LayoutMetrics.LOOKUP_CORNER_RADIUS;
            case COMMENT -> LayoutMetrics.COMMENT_CORNER_RADIUS;
            case CLD_VARIABLE -> LayoutMetrics.CLD_VAR_CORNER_RADIUS;
            case FLOW -> 2;
        };
    }
}

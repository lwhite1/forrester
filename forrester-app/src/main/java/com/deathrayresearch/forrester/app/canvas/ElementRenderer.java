package com.deathrayresearch.forrester.app.canvas;

import javafx.geometry.VPos;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

/**
 * Static methods that draw each element type onto a GraphicsContext,
 * following the Layered Flow Diagram visual specification.
 */
public final class ElementRenderer {

    private ElementRenderer() {
    }

    /**
     * Draws a stock: heavy rounded rectangle with centered name and unit badge.
     */
    public static void drawStock(GraphicsContext gc, String name, String unit,
                                 double x, double y, double width, double height) {
        double r = LayoutMetrics.STOCK_CORNER_RADIUS;

        // Fill
        gc.setFill(ColorPalette.STOCK_FILL);
        gc.fillRoundRect(x, y, width, height, r, r);

        // Border
        gc.setStroke(ColorPalette.STOCK_BORDER);
        gc.setLineWidth(LayoutMetrics.STOCK_BORDER_WIDTH);
        gc.setLineDashes();
        gc.strokeRoundRect(x, y, width, height, r, r);

        // Name centered (truncated to fit)
        gc.setFill(ColorPalette.TEXT);
        gc.setFont(LayoutMetrics.STOCK_NAME_FONT);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.fillText(truncate(name, LayoutMetrics.STOCK_NAME_FONT, width - 12),
                x + width / 2, y + height / 2);

        // Unit badge bottom-right
        if (unit != null && !unit.isBlank()) {
            gc.setFill(ColorPalette.TEXT_SECONDARY);
            gc.setFont(LayoutMetrics.BADGE_FONT);
            gc.setTextAlign(TextAlignment.RIGHT);
            gc.setTextBaseline(VPos.BOTTOM);
            gc.fillText(unit, x + width - 6, y + height - 4);
        }
    }

    /**
     * Draws a flow process indicator: small rounded diamond with name label and equation.
     *
     * @param x        top-left X
     * @param y        top-left Y
     * @param width    bounding box width
     * @param height   bounding box height
     */
    public static void drawFlow(GraphicsContext gc, String name,
                                double x, double y, double width, double height) {
        double cx = x + width / 2;
        double cy = y + height / 2;
        double half = Math.min(width, height) / 2;

        // Diamond shape (rotated square with rounded appearance)
        double[] xPoints = {cx, cx + half, cx, cx - half};
        double[] yPoints = {cy - half, cy, cy + half, cy};

        gc.setFill(ColorPalette.STOCK_FILL);
        gc.fillPolygon(xPoints, yPoints, 4);
        gc.setStroke(ColorPalette.AUX_BORDER);
        gc.setLineWidth(1.5);
        gc.setLineDashes();
        gc.strokePolygon(xPoints, yPoints, 4);

        // Name below the diamond (truncated to reasonable width)
        gc.setFill(ColorPalette.TEXT);
        gc.setFont(LayoutMetrics.FLOW_NAME_FONT);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.TOP);
        gc.fillText(truncate(name, LayoutMetrics.FLOW_NAME_FONT, LayoutMetrics.FLOW_LABEL_MAX_WIDTH),
                cx, cy + half + LayoutMetrics.FLOW_NAME_GAP);
    }

    /**
     * Draws an auxiliary variable: medium rounded rectangle with "fx" badge, centered name,
     */
    public static void drawAux(GraphicsContext gc, String name,
                               double x, double y, double width, double height) {
        double r = LayoutMetrics.AUX_CORNER_RADIUS;

        // Fill
        gc.setFill(ColorPalette.STOCK_FILL);
        gc.fillRoundRect(x, y, width, height, r, r);

        // Border
        gc.setStroke(ColorPalette.AUX_BORDER);
        gc.setLineWidth(LayoutMetrics.AUX_BORDER_WIDTH);
        gc.setLineDashes();
        gc.strokeRoundRect(x, y, width, height, r, r);

        // "fx" badge top-left
        gc.setFill(ColorPalette.TEXT_SECONDARY);
        gc.setFont(LayoutMetrics.BADGE_FONT);
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setTextBaseline(VPos.TOP);
        gc.fillText("fx", x + 5, y + 3);

        // Name centered (truncated to fit)
        gc.setFill(ColorPalette.TEXT);
        gc.setFont(LayoutMetrics.AUX_NAME_FONT);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.fillText(truncate(name, LayoutMetrics.AUX_NAME_FONT, width - 20),
                x + width / 2, y + height / 2);
    }

    /**
     * Draws a constant: small dashed-border rectangle with pin badge and name/value.
     */
    public static void drawConstant(GraphicsContext gc, String name, double value,
                                    double x, double y, double width, double height) {
        double r = LayoutMetrics.CONSTANT_CORNER_RADIUS;

        // Fill
        gc.setFill(ColorPalette.STOCK_FILL);
        gc.fillRoundRect(x, y, width, height, r, r);

        // Dashed border
        gc.setStroke(ColorPalette.CONSTANT_BORDER);
        gc.setLineWidth(LayoutMetrics.CONSTANT_BORDER_WIDTH);
        gc.setLineDashes(LayoutMetrics.CONSTANT_DASH_LENGTH, LayoutMetrics.CONSTANT_DASH_GAP);
        gc.strokeRoundRect(x, y, width, height, r, r);
        gc.setLineDashes();

        // Pin badge top-left
        gc.setFill(ColorPalette.TEXT_SECONDARY);
        gc.setFont(LayoutMetrics.BADGE_FONT);
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setTextBaseline(VPos.TOP);
        gc.fillText("pin", x + 4, y + 3);

        // Name centered, slightly above middle (truncated to fit)
        gc.setFill(ColorPalette.TEXT);
        gc.setFont(LayoutMetrics.CONSTANT_NAME_FONT);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.fillText(truncate(name, LayoutMetrics.CONSTANT_NAME_FONT, width - 16),
                x + width / 2, y + height / 2 + LayoutMetrics.LABEL_NAME_OFFSET);

        // Value below name
        gc.setFill(ColorPalette.TEXT_SECONDARY);
        gc.setFont(LayoutMetrics.BADGE_FONT);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.fillText(formatValue(value), x + width / 2, y + height / 2 + LayoutMetrics.LABEL_SUBLABEL_OFFSET);
    }

    /**
     * Draws a module: thick-bordered rounded rectangle with "mod" badge and centered name.
     */
    public static void drawModule(GraphicsContext gc, String name,
                                  double x, double y, double width, double height) {
        double r = LayoutMetrics.MODULE_CORNER_RADIUS;

        // Fill
        gc.setFill(ColorPalette.STOCK_FILL);
        gc.fillRoundRect(x, y, width, height, r, r);

        // Border
        gc.setStroke(ColorPalette.STOCK_BORDER);
        gc.setLineWidth(LayoutMetrics.MODULE_BORDER_WIDTH);
        gc.setLineDashes();
        gc.strokeRoundRect(x, y, width, height, r, r);

        // "mod" badge top-left
        gc.setFill(ColorPalette.TEXT_SECONDARY);
        gc.setFont(LayoutMetrics.BADGE_FONT);
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setTextBaseline(VPos.TOP);
        gc.fillText("mod", x + 5, y + 3);

        // Name centered (truncated to fit)
        gc.setFill(ColorPalette.TEXT);
        gc.setFont(LayoutMetrics.MODULE_NAME_FONT);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.fillText(truncate(name, LayoutMetrics.MODULE_NAME_FONT, width - 12),
                x + width / 2, y + height / 2);
    }

    /**
     * Draws a lookup table: rounded rectangle with dot-dash border, "tbl" badge,
     * name, and data point count.
     */
    public static void drawLookup(GraphicsContext gc, String name, int dataPoints,
                                  double x, double y, double width, double height) {
        double r = LayoutMetrics.LOOKUP_CORNER_RADIUS;

        // Fill
        gc.setFill(ColorPalette.STOCK_FILL);
        gc.fillRoundRect(x, y, width, height, r, r);

        // Dot-dash border
        gc.setStroke(ColorPalette.AUX_BORDER);
        gc.setLineWidth(LayoutMetrics.LOOKUP_BORDER_WIDTH);
        gc.setLineDashes(8, 3, 2, 3);
        gc.strokeRoundRect(x, y, width, height, r, r);
        gc.setLineDashes();

        // "tbl" badge top-left
        gc.setFill(ColorPalette.TEXT_SECONDARY);
        gc.setFont(LayoutMetrics.BADGE_FONT);
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setTextBaseline(VPos.TOP);
        gc.fillText("tbl", x + 4, y + 3);

        // Name centered, slightly above middle (truncated to fit)
        gc.setFill(ColorPalette.TEXT);
        gc.setFont(LayoutMetrics.LOOKUP_NAME_FONT);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.fillText(truncate(name, LayoutMetrics.LOOKUP_NAME_FONT, width - 16),
                x + width / 2, y + height / 2 + LayoutMetrics.LABEL_NAME_OFFSET);

        // Data point count below name
        gc.setFill(ColorPalette.TEXT_SECONDARY);
        gc.setFont(LayoutMetrics.BADGE_FONT);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.fillText(dataPoints + " pts", x + width / 2,
                y + height / 2 + LayoutMetrics.LABEL_SUBLABEL_OFFSET);
    }

    /**
     * Draws a CLD variable as plain text (no rectangle), matching standard CLD notation.
     */
    public static void drawCldVariable(GraphicsContext gc, String name,
                                       double x, double y, double width, double height) {
        gc.setFill(ColorPalette.TEXT);
        gc.setFont(LayoutMetrics.AUX_NAME_FONT);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.fillText(truncate(name, LayoutMetrics.AUX_NAME_FONT, width - 12),
                x + width / 2, y + height / 2);
    }

    /**
     * Returns true if the equation should be displayed on the canvas.
     * Suppresses null, blank, and the default placeholder "0".
     */
    static boolean isDisplayableEquation(String equation) {
        return equation != null && !equation.isBlank() && !"0".equals(equation.strip());
    }

    static String formatValue(double value) {
        if (Double.isNaN(value)) {
            return "NaN";
        }
        if (value == Math.floor(value) && !Double.isInfinite(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }

    /**
     * Truncates a name to fit within the given pixel width, appending "..." if needed.
     * Uses a shared {@link Text} node for measurement.
     */
    static String truncate(String name, Font font, double maxWidth) {
        Text text = new Text(name);
        text.setFont(font);
        if (text.getLayoutBounds().getWidth() <= maxWidth) {
            return name;
        }
        String ellipsis = "\u2026";
        for (int end = name.length() - 1; end > 0; end--) {
            String candidate = name.substring(0, end) + ellipsis;
            text.setText(candidate);
            if (text.getLayoutBounds().getWidth() <= maxWidth) {
                return candidate;
            }
        }
        return ellipsis;
    }
}

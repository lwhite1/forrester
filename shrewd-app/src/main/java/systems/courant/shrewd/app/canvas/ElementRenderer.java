package systems.courant.shrewd.app.canvas;

import javafx.geometry.VPos;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

import java.util.List;

/**
 * Static methods that draw each element type onto a GraphicsContext,
 * following the Layered Flow Diagram visual specification.
 */
public final class ElementRenderer {

    /** Badge label for formula-valued auxiliaries. */
    static final String BADGE_FORMULA = "fx";
    /** Badge label for lookup table elements. */
    static final String BADGE_LOOKUP = "Table";
    /** Badge label for module instances. */
    static final String BADGE_MODULE = "Module";
    /** Badge label for elements containing delay functions. */
    static final String BADGE_DELAY = "D";

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
    public static void drawFlow(GraphicsContext gc, String name, boolean hasDelay,
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

        // Delay badge top-right of diamond
        if (hasDelay) {
            gc.setFill(ColorPalette.DELAY_BADGE);
            gc.setFont(LayoutMetrics.BADGE_FONT);
            gc.setTextAlign(TextAlignment.LEFT);
            gc.setTextBaseline(VPos.BOTTOM);
            gc.fillText(BADGE_DELAY, cx + half + 2, cy - half + 4);
        }
    }

    /**
     * Draws an auxiliary variable: medium rounded rectangle with badge and centered name.
     * Literal-valued auxiliaries get a dashed border and show the numeric value as badge;
     * formula-valued auxiliaries get a solid border and show "fx" as badge.
     *
     * @param isLiteral true if the auxiliary's equation is a numeric literal
     * @param equation  the equation string (used to display value for literals)
     * @param hasDelay  true if the equation contains a delay function
     */
    public static void drawAux(GraphicsContext gc, String name, boolean isLiteral, String equation,
                               boolean hasDelay,
                               double x, double y, double width, double height) {
        double r = LayoutMetrics.AUX_CORNER_RADIUS;

        // Fill
        gc.setFill(ColorPalette.STOCK_FILL);
        gc.fillRoundRect(x, y, width, height, r, r);

        // Border: dashed for literals, solid for formulas
        if (isLiteral) {
            gc.setStroke(ColorPalette.AUX_LITERAL_BORDER);
            gc.setLineWidth(LayoutMetrics.AUX_BORDER_WIDTH);
            gc.setLineDashes(LayoutMetrics.AUX_LITERAL_DASH_LENGTH,
                    LayoutMetrics.AUX_LITERAL_DASH_GAP);
        } else {
            gc.setStroke(ColorPalette.AUX_BORDER);
            gc.setLineWidth(LayoutMetrics.AUX_BORDER_WIDTH);
            gc.setLineDashes();
        }
        gc.strokeRoundRect(x, y, width, height, r, r);
        gc.setLineDashes();

        // Badge top-left: value for literals, "fx" for formulas
        gc.setFill(ColorPalette.TEXT_SECONDARY);
        gc.setFont(LayoutMetrics.BADGE_FONT);
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setTextBaseline(VPos.TOP);
        if (isLiteral && equation != null) {
            try {
                double value = Double.parseDouble(equation.strip());
                gc.fillText(formatValue(value), x + 5, y + 3);
            } catch (NumberFormatException e) {
                gc.fillText(equation.strip(), x + 5, y + 3);
            }
        } else {
            gc.fillText(BADGE_FORMULA, x + 5, y + 3);
        }

        // Name centered (truncated to fit)
        gc.setFill(ColorPalette.TEXT);
        gc.setFont(LayoutMetrics.AUX_NAME_FONT);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.fillText(truncate(name, LayoutMetrics.AUX_NAME_FONT, width - 20),
                x + width / 2, y + height / 2);

        // Delay badge top-right
        if (hasDelay) {
            gc.setFill(ColorPalette.DELAY_BADGE);
            gc.setFont(LayoutMetrics.BADGE_FONT);
            gc.setTextAlign(TextAlignment.RIGHT);
            gc.setTextBaseline(VPos.TOP);
            gc.fillText(BADGE_DELAY, x + width - 5, y + 3);
        }
    }

    /**
     * Draws a module: thick-bordered rounded rectangle with "Module" badge, centered name,
     * and port indicators on the left (inputs) and right (outputs) edges.
     */
    public static void drawModule(GraphicsContext gc, String name,
                                  List<String> inputPorts, List<String> outputPorts,
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

        // Module badge top-left
        gc.setFill(ColorPalette.TEXT_SECONDARY);
        gc.setFont(LayoutMetrics.BADGE_FONT);
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setTextBaseline(VPos.TOP);
        gc.fillText(BADGE_MODULE, x + 5, y + 3);

        // Name centered (truncated to fit)
        gc.setFill(ColorPalette.TEXT);
        gc.setFont(LayoutMetrics.MODULE_NAME_FONT);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.fillText(truncate(name, LayoutMetrics.MODULE_NAME_FONT, width - 12),
                x + width / 2, y + height / 2);

        // Port indicators
        drawPortIndicators(gc, inputPorts, x, y, height, true);
        drawPortIndicators(gc, outputPorts, x + width, y, height, false);
    }

    private static final double PORT_RADIUS = 3.0;
    private static final Color PORT_COLOR = Color.web("#5B9BD5");
    private static final Font PORT_FONT = Font.font("System", 9);

    private static void drawPortIndicators(GraphicsContext gc, List<String> ports,
                                           double edgeX, double y, double height,
                                           boolean isInput) {
        if (ports == null || ports.isEmpty()) {
            return;
        }
        double spacing = height / (ports.size() + 1);
        for (int i = 0; i < ports.size(); i++) {
            double py = y + spacing * (i + 1);

            // Small circle on the edge
            gc.setFill(PORT_COLOR);
            gc.fillOval(edgeX - PORT_RADIUS, py - PORT_RADIUS,
                    PORT_RADIUS * 2, PORT_RADIUS * 2);
            gc.setStroke(Color.WHITE);
            gc.setLineWidth(1);
            gc.strokeOval(edgeX - PORT_RADIUS, py - PORT_RADIUS,
                    PORT_RADIUS * 2, PORT_RADIUS * 2);

            // Port name label
            gc.setFill(ColorPalette.TEXT_SECONDARY);
            gc.setFont(PORT_FONT);
            gc.setTextBaseline(VPos.CENTER);
            if (isInput) {
                gc.setTextAlign(TextAlignment.LEFT);
                gc.fillText(ports.get(i), edgeX + PORT_RADIUS + 3, py);
            } else {
                gc.setTextAlign(TextAlignment.RIGHT);
                gc.fillText(ports.get(i), edgeX - PORT_RADIUS - 3, py);
            }
        }
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

        // Table badge top-left
        gc.setFill(ColorPalette.TEXT_SECONDARY);
        gc.setFont(LayoutMetrics.BADGE_FONT);
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setTextBaseline(VPos.TOP);
        gc.fillText(BADGE_LOOKUP, x + 4, y + 3);

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
     * Draws a comment annotation box: light yellow fill with a subtle amber border,
     * styled as a sticky-note. Wraps text within the bounding box.
     */
    public static void drawComment(GraphicsContext gc, String text,
                                   double x, double y, double width, double height) {
        double r = LayoutMetrics.COMMENT_CORNER_RADIUS;

        // Fill — warm yellow note appearance
        gc.setFill(ColorPalette.COMMENT_FILL);
        gc.fillRoundRect(x, y, width, height, r, r);

        // Border
        gc.setStroke(ColorPalette.COMMENT_BORDER);
        gc.setLineWidth(LayoutMetrics.COMMENT_BORDER_WIDTH);
        gc.setLineDashes();
        gc.strokeRoundRect(x, y, width, height, r, r);

        // Text content (wrapped, top-left aligned)
        if (text != null && !text.isBlank()) {
            gc.setFill(ColorPalette.TEXT);
            gc.setFont(LayoutMetrics.COMMENT_TEXT_FONT);
            gc.setTextAlign(TextAlignment.LEFT);
            gc.setTextBaseline(VPos.TOP);
            String display = truncate(text, LayoutMetrics.COMMENT_TEXT_FONT, width - 12);
            gc.fillText(display, x + 6, y + 6, width - 12);
        }
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
        Text measureText = new Text(name);
        measureText.setFont(font);
        if (measureText.getLayoutBounds().getWidth() <= maxWidth) {
            return name;
        }
        String ellipsis = "\u2026";
        for (int end = name.length() - 1; end > 0; end--) {
            String candidate = name.substring(0, end) + ellipsis;
            measureText.setText(candidate);
            if (measureText.getLayoutBounds().getWidth() <= maxWidth) {
                return candidate;
            }
        }
        return ellipsis;
    }
}

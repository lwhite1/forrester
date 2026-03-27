package systems.courant.sd.app.canvas.renderers;

import javafx.geometry.VPos;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

import java.util.ArrayList;
import java.util.List;
import systems.courant.sd.app.canvas.ColorPalette;
import systems.courant.sd.app.canvas.LayoutMetrics;
import systems.courant.sd.app.canvas.PortGeometry;

/**
 * Static methods that draw each element type onto a GraphicsContext,
 * following the Layered Flow Diagram visual specification.
 */
public final class ElementRenderer {

    /** Badge label for formula-valued variables. */
    public static final String BADGE_FORMULA = "fx";
    /** Badge label for lookup table elements. */
    public static final String BADGE_LOOKUP = "Table";
    /** Badge label for module instances. */
    public static final String BADGE_MODULE = "Module";
    /** Badge label for elements containing delay functions. */
    public static final String BADGE_DELAY = "D";
    /** Badge prefix for subscript dimension indicators. */
    public static final String BADGE_SUBSCRIPT_PREFIX = "\u2193";

    private ElementRenderer() {
    }

    /**
     * Draws a subscript dimension badge at the bottom-right of an element.
     * Shows dimension names joined by " x " (e.g., "Region" or "Region x Age").
     * Does nothing if the subscript list is empty.
     */
    public static void drawSubscriptBadge(GraphicsContext gc, List<String> subscripts,
                                           double x, double y, double width, double height) {
        if (subscripts == null || subscripts.isEmpty()) {
            return;
        }
        String label = String.join(" \u00d7 ", subscripts);
        gc.setFill(ColorPalette.SUBSCRIPT_BADGE);
        gc.setFont(LayoutMetrics.BADGE_FONT);
        gc.setTextAlign(TextAlignment.RIGHT);
        gc.setTextBaseline(VPos.BOTTOM);
        gc.fillText(label, x + width - 4, y + height - 3);
    }

    /**
     * Draws a stock (scalar, no subscript badge).
     */
    public static void drawStock(GraphicsContext gc, String name, String unit,
                                 double x, double y, double width, double height) {
        drawStock(gc, name, unit, List.of(), x, y, width, height);
    }

    /**
     * Draws a stock: heavy rounded rectangle with centered name, unit badge,
     * and optional subscript badge.
     */
    public static void drawStock(GraphicsContext gc, String name, String unit,
                                 List<String> subscripts,
                                 double x, double y, double width, double height) {
        drawStock(gc, name, unit, subscripts, x, y, width, height, null);
    }

    /**
     * Draws a stock with an optional custom color override for border and text.
     */
    public static void drawStock(GraphicsContext gc, String name, String unit,
                                 List<String> subscripts,
                                 double x, double y, double width, double height,
                                 Color customColor) {
        double r = LayoutMetrics.STOCK_CORNER_RADIUS;
        Color borderColor = customColor != null ? customColor : ColorPalette.STOCK_BORDER;
        Color textColor = customColor != null ? customColor : ColorPalette.TEXT;

        // Fill
        gc.setFill(ColorPalette.STOCK_FILL);
        gc.fillRoundRect(x, y, width, height, r, r);

        // Border
        gc.setStroke(borderColor);
        gc.setLineWidth(LayoutMetrics.STOCK_BORDER_WIDTH);
        gc.setLineDashes();
        gc.strokeRoundRect(x, y, width, height, r, r);

        // Name centered (truncated to fit)
        gc.setFill(textColor);
        gc.setFont(LayoutMetrics.STOCK_NAME_FONT);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.fillText(truncate(name, LayoutMetrics.STOCK_NAME_FONT, width - 12),
                x + width / 2, y + height / 2);

        // Unit badge centered below name
        if (unit != null && !unit.isBlank()) {
            gc.setFill(customColor != null ? customColor : ColorPalette.BADGE_TEXT);
            gc.setFont(LayoutMetrics.UNIT_BADGE_FONT);
            gc.setTextAlign(TextAlignment.CENTER);
            gc.setTextBaseline(VPos.BOTTOM);
            gc.fillText("[" + unit + "]", x + width / 2, y + height - 3);
        }

        // Subscript badge bottom-right
        drawSubscriptBadge(gc, subscripts, x, y, width, height);
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
        drawFlow(gc, name, hasDelay, List.of(), x, y, width, height);
    }

    public static void drawFlow(GraphicsContext gc, String name, boolean hasDelay,
                                List<String> subscripts,
                                double x, double y, double width, double height) {
        drawFlow(gc, name, hasDelay, subscripts, x, y, width, height, null);
    }

    /**
     * Draws a flow with an optional custom color override for border and text.
     */
    public static void drawFlow(GraphicsContext gc, String name, boolean hasDelay,
                                List<String> subscripts,
                                double x, double y, double width, double height,
                                Color customColor) {
        double cx = x + width / 2;
        double cy = y + height / 2;
        double half = Math.min(width, height) / 2;
        Color borderColor = customColor != null ? customColor : ColorPalette.AUX_BORDER;
        Color textColor = customColor != null ? customColor : ColorPalette.TEXT;

        // Diamond shape (rotated square with rounded appearance)
        double[] xPoints = {cx, cx + half, cx, cx - half};
        double[] yPoints = {cy - half, cy, cy + half, cy};

        gc.setFill(ColorPalette.ELEMENT_FILL);
        gc.fillPolygon(xPoints, yPoints, 4);
        gc.setStroke(borderColor);
        gc.setLineWidth(1.5);
        gc.setLineDashes();
        gc.strokePolygon(xPoints, yPoints, 4);

        // Name below the diamond (truncated to reasonable width)
        gc.setFill(textColor);
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

        // Subscript badge right of name
        if (subscripts != null && !subscripts.isEmpty()) {
            String label = String.join(" \u00d7 ", subscripts);
            gc.setFill(ColorPalette.SUBSCRIPT_BADGE);
            gc.setFont(LayoutMetrics.BADGE_FONT);
            gc.setTextAlign(TextAlignment.CENTER);
            gc.setTextBaseline(VPos.TOP);
            double nameY = cy + half + LayoutMetrics.FLOW_NAME_GAP;
            gc.fillText(label, cx, nameY + 12);
        }
    }

    /**
     * Draws a variable variable: medium rounded rectangle with badge and centered name.
     * Literal-valued variables get a dashed border and show the numeric value as badge;
     * formula-valued variables get a solid border and show "fx" as badge.
     *
     * @param isLiteral true if the variable's equation is a numeric literal
     * @param equation  the equation string (used to display value for literals)
     * @param hasDelay  true if the equation contains a delay function
     */
    public static void drawAux(GraphicsContext gc, String name, boolean isLiteral, String equation,
                               boolean hasDelay,
                               double x, double y, double width, double height) {
        drawAux(gc, name, isLiteral, equation, hasDelay, List.of(), x, y, width, height, false);
    }

    public static void drawAux(GraphicsContext gc, String name, boolean isLiteral, String equation,
                               boolean hasDelay, List<String> subscripts,
                               double x, double y, double width, double height) {
        drawAux(gc, name, isLiteral, equation, hasDelay, subscripts, x, y, width, height, false);
    }

    /**
     * Draws a variable: rounded rectangle with subtle fill (no border), badge, and centered name.
     * When hovered, uses a stronger fill to provide visual feedback.
     */
    public static void drawAux(GraphicsContext gc, String name, boolean isLiteral, String equation,
                               boolean hasDelay, List<String> subscripts,
                               double x, double y, double width, double height,
                               boolean hovered) {
        drawAux(gc, name, isLiteral, equation, hasDelay, subscripts, x, y, width, height, hovered, null);
    }

    /**
     * Draws a variable with an optional custom color override for text.
     */
    public static void drawAux(GraphicsContext gc, String name, boolean isLiteral, String equation,
                               boolean hasDelay, List<String> subscripts,
                               double x, double y, double width, double height,
                               boolean hovered, Color customColor) {
        double r = LayoutMetrics.AUX_CORNER_RADIUS;
        Color textColor = customColor != null ? customColor : ColorPalette.TEXT;
        Color badgeColor = customColor != null ? customColor : ColorPalette.BADGE_TEXT;

        // Fill: hover fill when hovered, subtle gray otherwise
        gc.setFill(hovered ? ColorPalette.HOVER_FILL : ColorPalette.VARIABLE_FILL);
        gc.fillRoundRect(x, y, width, height, r, r);

        // Badge top-left: value for literals, "fx" for formulas
        gc.setFill(badgeColor);
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

        // Name centered — wrap to two lines if needed
        gc.setFill(textColor);
        gc.setFont(LayoutMetrics.AUX_NAME_FONT);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        drawWrappedName(gc, name, LayoutMetrics.AUX_NAME_FONT, x, y, width, height, 20);

        // Delay badge top-right
        if (hasDelay) {
            gc.setFill(ColorPalette.DELAY_BADGE);
            gc.setFont(LayoutMetrics.BADGE_FONT);
            gc.setTextAlign(TextAlignment.RIGHT);
            gc.setTextBaseline(VPos.TOP);
            gc.fillText(BADGE_DELAY, x + width - 5, y + 3);
        }

        // Subscript badge bottom-right
        drawSubscriptBadge(gc, subscripts, x, y, width, height);
    }

    /**
     * Draws a module: thick-bordered rounded rectangle with "Module" badge, centered name,
     * and port indicators on the left (inputs) and right (outputs) edges.
     */
    public static void drawModule(GraphicsContext gc, String name,
                                  List<String> inputPorts, List<String> outputPorts,
                                  double x, double y, double width, double height) {
        drawModule(gc, name, inputPorts, outputPorts, x, y, width, height, null);
    }

    /**
     * Draws a module with an optional custom color override for border and text.
     */
    public static void drawModule(GraphicsContext gc, String name,
                                  List<String> inputPorts, List<String> outputPorts,
                                  double x, double y, double width, double height,
                                  Color customColor) {
        double r = LayoutMetrics.MODULE_CORNER_RADIUS;
        Color borderColor = customColor != null ? customColor : ColorPalette.STOCK_BORDER;
        Color textColor = customColor != null ? customColor : ColorPalette.TEXT;

        // Fill
        gc.setFill(ColorPalette.ELEMENT_FILL);
        gc.fillRoundRect(x, y, width, height, r, r);

        // Border
        gc.setStroke(borderColor);
        gc.setLineWidth(LayoutMetrics.MODULE_BORDER_WIDTH);
        gc.setLineDashes();
        gc.strokeRoundRect(x, y, width, height, r, r);

        // Module badge top-left
        gc.setFill(customColor != null ? customColor : ColorPalette.BADGE_TEXT);
        gc.setFont(LayoutMetrics.BADGE_FONT);
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setTextBaseline(VPos.TOP);
        gc.fillText(BADGE_MODULE, x + 5, y + 3);

        // Name centered (truncated to fit)
        gc.setFill(textColor);
        gc.setFont(LayoutMetrics.MODULE_NAME_FONT);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.fillText(truncate(name, LayoutMetrics.MODULE_NAME_FONT, width - 12),
                x + width / 2, y + height / 2);

        // Port indicators
        drawPortIndicators(gc, inputPorts, x, y, height, true);
        drawPortIndicators(gc, outputPorts, x + width, y, height, false);
    }

    /** Use {@link PortGeometry#PORT_RADIUS} for shared access. */
    private static final double PORT_RADIUS = PortGeometry.PORT_RADIUS;
    private static final Color PORT_COLOR = Color.web("#5B9BD5");
    private static final Font PORT_FONT = Font.font("System", 9);

    private static void drawPortIndicators(GraphicsContext gc, List<String> ports,
                                           double edgeX, double y, double height,
                                           boolean isInput) {
        if (ports == null || ports.isEmpty()) {
            return;
        }
        for (int i = 0; i < ports.size(); i++) {
            double py = PortGeometry.portY(y, height, i, ports.size());

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
    public static void drawLookup(GraphicsContext gc, String name,
                                  double x, double y, double width, double height) {
        drawLookup(gc, name, x, y, width, height, false);
    }

    /**
     * Draws a lookup table: rounded rectangle with subtle fill (no border),
     * "Table" badge, and centered name. When hovered, uses a stronger fill.
     */
    public static void drawLookup(GraphicsContext gc, String name,
                                  double x, double y, double width, double height,
                                  boolean hovered) {
        drawLookup(gc, name, x, y, width, height, hovered, null);
    }

    /**
     * Draws a lookup table with an optional custom color override for text.
     */
    public static void drawLookup(GraphicsContext gc, String name,
                                  double x, double y, double width, double height,
                                  boolean hovered, Color customColor) {
        double r = LayoutMetrics.LOOKUP_CORNER_RADIUS;
        Color textColor = customColor != null ? customColor : ColorPalette.TEXT;
        Color badgeColor = customColor != null ? customColor : ColorPalette.BADGE_TEXT;

        // Fill: hover fill when hovered, subtle gray otherwise
        gc.setFill(hovered ? ColorPalette.HOVER_FILL : ColorPalette.LOOKUP_FILL);
        gc.fillRoundRect(x, y, width, height, r, r);

        // Table badge top-left
        gc.setFill(badgeColor);
        gc.setFont(LayoutMetrics.BADGE_FONT);
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setTextBaseline(VPos.TOP);
        gc.fillText(BADGE_LOOKUP, x + 4, y + 3);

        // Name centered vertically — wrap to two lines if needed
        gc.setFill(textColor);
        gc.setFont(LayoutMetrics.LOOKUP_NAME_FONT);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        drawWrappedName(gc, name, LayoutMetrics.LOOKUP_NAME_FONT, x, y, width, height, 16);
    }

    /** Padding inside the comment box (each side). */
    public static final double COMMENT_PADDING = 6;

    /**
     * Draws a comment annotation box: light yellow fill with a subtle amber border,
     * styled as a sticky-note. Wraps text within the bounding box across multiple lines.
     */
    public static void drawComment(GraphicsContext gc, String text,
                                   double x, double y, double width, double height) {
        drawComment(gc, text, x, y, width, height, null);
    }

    /**
     * Draws a comment with an optional custom color override for accent and text.
     */
    public static void drawComment(GraphicsContext gc, String text,
                                   double x, double y, double width, double height,
                                   Color customColor) {
        double r = LayoutMetrics.COMMENT_CORNER_RADIUS;

        // Left accent bar
        gc.setFill(customColor != null ? customColor : ColorPalette.COMMENT_ACCENT);
        gc.fillRect(x, y + r, LayoutMetrics.COMMENT_ACCENT_WIDTH, height - r * 2);

        // Text content (word-wrapped, top-left aligned)
        if (text != null && !text.isBlank()) {
            gc.setFill(customColor != null ? customColor : ColorPalette.COMMENT_TEXT);
            gc.setFont(LayoutMetrics.COMMENT_TEXT_FONT);
            gc.setTextAlign(TextAlignment.LEFT);
            gc.setTextBaseline(VPos.TOP);

            double maxLineWidth = width - COMMENT_PADDING * 2;
            double lineHeight = measureLineHeight(LayoutMetrics.COMMENT_TEXT_FONT);
            List<String> lines = wrapText(text, LayoutMetrics.COMMENT_TEXT_FONT, maxLineWidth);
            int maxVisibleLines = Math.max(1, (int) ((height - COMMENT_PADDING * 2) / lineHeight));

            for (int i = 0; i < Math.min(lines.size(), maxVisibleLines); i++) {
                String line = lines.get(i);
                // Truncate last visible line with ellipsis if more text follows
                if (i == maxVisibleLines - 1 && lines.size() > maxVisibleLines) {
                    line = truncate(line + "\u2026", LayoutMetrics.COMMENT_TEXT_FONT, maxLineWidth);
                }
                gc.fillText(line, x + COMMENT_PADDING, y + COMMENT_PADDING + i * lineHeight);
            }
        }
    }

    /**
     * Draws a CLD variable as plain text (no rectangle), matching standard CLD notation.
     * Wraps long names to multiple lines (up to 3) to fit within the element width.
     */
    public static void drawCldVariable(GraphicsContext gc, String name,
                                       double x, double y, double width, double height) {
        drawCldVariable(gc, name, x, y, width, height, null);
    }

    /**
     * Draws a CLD variable with an optional custom color override for text.
     */
    public static void drawCldVariable(GraphicsContext gc, String name,
                                       double x, double y, double width, double height,
                                       Color customColor) {
        gc.setFill(customColor != null ? customColor : ColorPalette.TEXT);
        gc.setFont(LayoutMetrics.AUX_NAME_FONT);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        drawWrappedCldName(gc, name, LayoutMetrics.AUX_NAME_FONT,
                x, y, width, height, LayoutMetrics.CLD_VAR_TEXT_PADDING);
    }

    /**
     * Returns true if the equation should be displayed on the canvas.
     * Suppresses null, blank, and the default placeholder "0".
     */
    public static boolean isDisplayableEquation(String equation) {
        return equation != null && !equation.isBlank() && !"0".equals(equation.strip());
    }

    public static String formatValue(double value) {
        if (Double.isNaN(value)) {
            return "NaN";
        }
        if (value == Math.floor(value) && !Double.isInfinite(value)
                && value >= Long.MIN_VALUE && value < (double) Long.MAX_VALUE) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }

    /**
     * Computes the bounding box size needed to display the given comment text,
     * wrapping at the default comment width. Returns {@code {width, height}}.
     *
     * <p>The returned width is the wider of the longest wrapped line and the minimum
     * comment width. The returned height fits all wrapped lines plus padding.
     */
    public static double[] computeCommentSize(String text) {
        if (text == null || text.isBlank()) {
            double minW = LayoutMetrics.minWidthFor(
                    systems.courant.sd.model.def.ElementType.COMMENT);
            double minH = LayoutMetrics.minHeightFor(
                    systems.courant.sd.model.def.ElementType.COMMENT);
            return new double[]{minW, minH};
        }
        double maxLineWidth = LayoutMetrics.COMMENT_WIDTH - COMMENT_PADDING * 2;
        List<String> lines = wrapText(text, LayoutMetrics.COMMENT_TEXT_FONT, maxLineWidth);
        double lineHeight = measureLineHeight(LayoutMetrics.COMMENT_TEXT_FONT);

        // Width: widest line + padding, clamped to [minWidth, COMMENT_WIDTH]
        double maxTextWidth = 0;
        MEASURE_TEXT.get().setFont(LayoutMetrics.COMMENT_TEXT_FONT);
        for (String line : lines) {
            MEASURE_TEXT.get().setText(line);
            maxTextWidth = Math.max(maxTextWidth, MEASURE_TEXT.get().getLayoutBounds().getWidth());
        }
        double minW = LayoutMetrics.minWidthFor(
                systems.courant.sd.model.def.ElementType.COMMENT);
        double w = Math.max(minW, Math.min(maxTextWidth + COMMENT_PADDING * 2,
                LayoutMetrics.COMMENT_WIDTH));

        // Height: lines * lineHeight + padding, clamped to minHeight
        double minH = LayoutMetrics.minHeightFor(
                systems.courant.sd.model.def.ElementType.COMMENT);
        double h = Math.max(minH, lines.size() * lineHeight + COMMENT_PADDING * 2);

        return new double[]{w, h};
    }

    /**
     * Word-wraps text into lines that fit within the given pixel width.
     * Respects explicit newlines in the input text.
     */
    public static List<String> wrapText(String text, Font font, double maxWidth) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return lines;
        }
        MEASURE_TEXT.get().setFont(font);

        for (String paragraph : text.split("\n", -1)) {
            if (paragraph.isEmpty()) {
                lines.add("");
                continue;
            }
            String[] words = paragraph.split("\\s+");
            StringBuilder current = new StringBuilder();
            for (String word : words) {
                if (current.isEmpty()) {
                    current.append(word);
                } else {
                    String candidate = current + " " + word;
                    MEASURE_TEXT.get().setText(candidate);
                    if (MEASURE_TEXT.get().getLayoutBounds().getWidth() > maxWidth) {
                        lines.add(current.toString());
                        current = new StringBuilder(word);
                    } else {
                        current.append(" ").append(word);
                    }
                }
            }
            lines.add(current.toString());
        }
        return lines;
    }

    /**
     * Returns the line height for the given font, measured using the shared Text node.
     */
    public static double measureLineHeight(Font font) {
        MEASURE_TEXT.get().setFont(font);
        MEASURE_TEXT.get().setText("Ay");
        return MEASURE_TEXT.get().getLayoutBounds().getHeight();
    }

    /** Thread-local Text node for width measurement — safe from any thread. */
    private static final ThreadLocal<Text> MEASURE_TEXT =
            ThreadLocal.withInitial(Text::new);

    /**
     * Draws a name centered in the element bounding box, wrapping to two lines if needed.
     * If the name fits on one line, draws centered as usual. If it needs wrapping, draws
     * up to 2 lines vertically centered. If more than 2 lines, truncates line 2 with ellipsis.
     *
     * @param padding  total horizontal padding (both sides combined)
     */
    private static void drawWrappedName(GraphicsContext gc, String name, Font font,
                                        double x, double y, double width, double height,
                                        double padding) {
        double maxWidth = width - padding;
        MEASURE_TEXT.get().setFont(font);
        MEASURE_TEXT.get().setText(name);
        if (MEASURE_TEXT.get().getLayoutBounds().getWidth() <= maxWidth) {
            // Fits on one line
            gc.fillText(name, x + width / 2, y + height / 2);
        } else {
            List<String> lines = wrapText(name, font, maxWidth);
            double lineHeight = measureLineHeight(font);
            int lineCount = Math.min(lines.size(), 2);
            double totalHeight = lineCount * lineHeight;
            double startY = y + (height - totalHeight) / 2 + lineHeight / 2;

            for (int i = 0; i < lineCount; i++) {
                String line = truncate(lines.get(i), font, maxWidth);
                gc.fillText(line, x + width / 2, startY + i * lineHeight);
            }
        }
    }

    /**
     * Draws a CLD variable name, wrapping to up to 3 lines (vs 2 for AUX elements)
     * to accommodate the longer names typical in causal loop diagrams.
     */
    private static void drawWrappedCldName(GraphicsContext gc, String name, Font font,
                                            double x, double y, double width, double height,
                                            double padding) {
        double maxWidth = width - padding;
        MEASURE_TEXT.get().setFont(font);
        MEASURE_TEXT.get().setText(name);
        if (MEASURE_TEXT.get().getLayoutBounds().getWidth() <= maxWidth) {
            gc.fillText(name, x + width / 2, y + height / 2);
        } else {
            List<String> lines = wrapText(name, font, maxWidth);
            double lineHeight = measureLineHeight(font);
            int lineCount = Math.min(lines.size(), 3);
            double totalHeight = lineCount * lineHeight;
            double startY = y + (height - totalHeight) / 2 + lineHeight / 2;

            for (int i = 0; i < lineCount; i++) {
                String line = (i == lineCount - 1 && lines.size() > lineCount)
                        ? truncate(lines.get(i), font, maxWidth)
                        : lines.get(i);
                gc.fillText(line, x + width / 2, startY + i * lineHeight);
            }
        }
    }

    /**
     * Truncates a name to fit within the given pixel width, appending "..." if needed.
     * Uses a shared {@link Text} node for measurement to avoid per-call allocation.
     */
    public static String truncate(String name, Font font, double maxWidth) {
        MEASURE_TEXT.get().setFont(font);
        MEASURE_TEXT.get().setText(name);
        if (MEASURE_TEXT.get().getLayoutBounds().getWidth() <= maxWidth) {
            return name;
        }
        String ellipsis = "\u2026";
        for (int end = name.length() - 1; end > 0; end--) {
            String candidate = name.substring(0, end) + ellipsis;
            MEASURE_TEXT.get().setText(candidate);
            if (MEASURE_TEXT.get().getLayoutBounds().getWidth() <= maxWidth) {
                return candidate;
            }
        }
        return ellipsis;
    }
}

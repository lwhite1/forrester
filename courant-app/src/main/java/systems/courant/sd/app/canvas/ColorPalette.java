package systems.courant.sd.app.canvas;

import javafx.scene.paint.Color;

/**
 * Layered Flow Diagram color constants.
 */
public final class ColorPalette {

    private ColorPalette() {
    }

    public static final Color STOCK_BORDER = Color.web("#2C3E50");
    public static final Color STOCK_FILL = Color.WHITE;
    public static final Color AUX_BORDER = Color.web("#7F8C8D");
    /** Border color for literal-valued variables (dashed). */
    public static final Color AUX_LITERAL_BORDER = Color.web("#BDC3C7");
    public static final Color MATERIAL_FLOW = Color.web("#2C3E50");
    public static final Color INFO_LINK = Color.web("#7F8C8D", 0.6);
    public static final Color SAME_DIRECTION = Color.web("#4A90D9");
    public static final Color OPPOSITE_DIRECTION = Color.web("#D97B4A");
    public static final Color BACKGROUND = Color.web("#F8F9FA");
    public static final Color TEXT = Color.web("#2C3E50");
    public static final Color TEXT_SECONDARY = Color.web("#7F8C8D");
    public static final Color CLOUD = Color.web("#BDC3C7");

    // CLD (Causal Loop Diagram) colors
    public static final Color CLD_VAR_BORDER = Color.web("#7F8C8D");
    public static final Color CAUSAL_LINK = Color.web("#2C3E50");
    public static final Color CAUSAL_POSITIVE = Color.web("#27AE60");
    public static final Color CAUSAL_NEGATIVE = Color.web("#E74C3C");
    public static final Color CAUSAL_UNKNOWN = Color.web("#BDC3C7");

    public static final Color HOVER = Color.web("#4A90D9", 0.4);
    public static final Color SELECTION = Color.web("#4A90D9", 0.8);
    public static final Color RUBBER_BAND = Color.web("#4A90D9", 0.6);
    public static final Color MARQUEE_FILL = Color.web("#4A90D9", 0.1);
    public static final Color PORT_HOVER = Color.web("#4A90D9", 0.35);

    // Validation error/warning indicators on canvas elements
    public static final Color ERROR_BORDER = Color.web("#E74C3C", 0.8);
    public static final Color ERROR_FILL = Color.web("#E74C3C", 0.06);
    public static final Color WARNING_BORDER = Color.web("#F39C12", 0.8);
    public static final Color WARNING_FILL = Color.web("#F39C12", 0.06);

    public static final Color LOOP_HIGHLIGHT = Color.web("#E74C3C", 0.8);
    public static final Color LOOP_EDGE = Color.web("#E74C3C", 0.6);
    public static final Color LOOP_FILL = Color.web("#E74C3C", 0.08);

    // Loop classification label colors
    public static final Color LOOP_REINFORCING = Color.web("#27AE60");
    public static final Color LOOP_BALANCING = Color.web("#E74C3C");
    public static final Color LOOP_INDETERMINATE = Color.web("#95A5A6");

    // Causal trace colors
    public static final Color TRACE_UPSTREAM = Color.web("#2980B9");
    public static final Color TRACE_DOWNSTREAM = Color.web("#E67E22");
    public static final Color TRACE_ORIGIN = Color.web("#8E44AD");

    // Comment annotation colors
    public static final Color COMMENT_FILL = Color.web("#FFFDE7");
    public static final Color COMMENT_BORDER = Color.web("#F9A825", 0.6);

    // Delay indicator badge
    public static final Color DELAY_BADGE = Color.web("#8E44AD");

    // Maturity indicators (#89)
    /** Amber accent stripe on elements missing equations. */
    public static final Color MATURITY_ACCENT = Color.web("#F39C12", 0.8);
    /** Color for the "?" unit badge on elements without units. */
    public static final Color MATURITY_UNIT_BADGE = Color.web("#F39C12");
    /** Red color for flow connection lines with unit mismatches. */
    public static final Color UNIT_MISMATCH = Color.web("#E74C3C", 0.8);
}

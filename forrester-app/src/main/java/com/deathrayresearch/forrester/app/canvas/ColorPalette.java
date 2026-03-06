package com.deathrayresearch.forrester.app.canvas;

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
    public static final Color CONSTANT_BORDER = Color.web("#BDC3C7");
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

    public static final Color LOOP_HIGHLIGHT = Color.web("#E74C3C", 0.8);
    public static final Color LOOP_EDGE = Color.web("#E74C3C", 0.6);
    public static final Color LOOP_FILL = Color.web("#E74C3C", 0.08);

    // Loop classification label colors
    public static final Color LOOP_REINFORCING = Color.web("#27AE60");
    public static final Color LOOP_BALANCING = Color.web("#E74C3C");
    public static final Color LOOP_INDETERMINATE = Color.web("#95A5A6");
}

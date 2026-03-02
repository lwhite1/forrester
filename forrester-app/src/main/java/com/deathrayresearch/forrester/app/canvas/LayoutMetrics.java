package com.deathrayresearch.forrester.app.canvas;

import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Element dimensions, spacing, and font metrics for the Layered Flow Diagram.
 */
public final class LayoutMetrics {

    private LayoutMetrics() {
    }

    // Stock dimensions
    public static final double STOCK_WIDTH = 140;
    public static final double STOCK_HEIGHT = 80;
    public static final double STOCK_BORDER_WIDTH = 3;
    public static final double STOCK_CORNER_RADIUS = 8;

    // Auxiliary dimensions
    public static final double AUX_WIDTH = 100;
    public static final double AUX_HEIGHT = 55;
    public static final double AUX_BORDER_WIDTH = 1.5;
    public static final double AUX_CORNER_RADIUS = 6;

    // Constant dimensions
    public static final double CONSTANT_WIDTH = 90;
    public static final double CONSTANT_HEIGHT = 45;
    public static final double CONSTANT_BORDER_WIDTH = 1;
    public static final double CONSTANT_CORNER_RADIUS = 4;
    public static final double CONSTANT_DASH_LENGTH = 6;
    public static final double CONSTANT_DASH_GAP = 4;

    // Flow indicator
    public static final double FLOW_INDICATOR_SIZE = 30;

    // Material flow line
    public static final double MATERIAL_FLOW_WIDTH = 4;
    public static final double ARROWHEAD_LENGTH = 14;
    public static final double ARROWHEAD_WIDTH = 10;

    // Info link line
    public static final double INFO_LINK_WIDTH = 1;
    public static final double INFO_LINK_DASH_LENGTH = 5;
    public static final double INFO_LINK_DASH_GAP = 4;
    public static final double INFO_ARROWHEAD_LENGTH = 8;
    public static final double INFO_ARROWHEAD_WIDTH = 6;

    // Cloud symbol
    public static final double CLOUD_RADIUS = 12;

    // Fonts
    public static final Font STOCK_NAME_FONT = Font.font("System", FontWeight.BOLD, 13);
    public static final Font AUX_NAME_FONT = Font.font("System", FontWeight.NORMAL, 12);
    public static final Font CONSTANT_NAME_FONT = Font.font("System", FontWeight.NORMAL, 11);
    public static final Font BADGE_FONT = Font.font("System", FontWeight.NORMAL, 9);
    public static final Font FLOW_NAME_FONT = Font.font("System", FontWeight.NORMAL, 11);

    /**
     * Returns the width for a given element type.
     */
    public static double widthFor(String type) {
        return switch (type) {
            case "stock" -> STOCK_WIDTH;
            case "aux" -> AUX_WIDTH;
            case "constant" -> CONSTANT_WIDTH;
            case "flow" -> FLOW_INDICATOR_SIZE;
            default -> AUX_WIDTH;
        };
    }

    /**
     * Returns the height for a given element type.
     */
    public static double heightFor(String type) {
        return switch (type) {
            case "stock" -> STOCK_HEIGHT;
            case "aux" -> AUX_HEIGHT;
            case "constant" -> CONSTANT_HEIGHT;
            case "flow" -> FLOW_INDICATOR_SIZE;
            default -> AUX_HEIGHT;
        };
    }
}

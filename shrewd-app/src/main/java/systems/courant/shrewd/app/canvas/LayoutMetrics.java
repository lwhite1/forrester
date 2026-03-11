package systems.courant.shrewd.app.canvas;

import systems.courant.shrewd.model.def.ElementType;

import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

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

    // Auxiliary literal (dashed border) dimensions
    public static final double AUX_LITERAL_DASH_LENGTH = 6;
    public static final double AUX_LITERAL_DASH_GAP = 4;

    // Lookup table dimensions
    public static final double LOOKUP_WIDTH = 100;
    public static final double LOOKUP_HEIGHT = 50;
    public static final double LOOKUP_BORDER_WIDTH = 1.5;
    public static final double LOOKUP_CORNER_RADIUS = 4;
    public static final Font LOOKUP_NAME_FONT = Font.font("System", FontWeight.NORMAL, 11);

    // CLD variable dimensions
    public static final double CLD_VAR_WIDTH = 110;
    public static final double CLD_VAR_HEIGHT = 30;
    public static final double CLD_VAR_BORDER_WIDTH = 1;
    public static final double CLD_VAR_CORNER_RADIUS = 6;
    /** Horizontal padding around text for CLD variable auto-sizing. */
    public static final double CLD_VAR_TEXT_PADDING = 16;

    // Causal link
    public static final double CAUSAL_LINK_WIDTH = 1.5;
    public static final double CAUSAL_ARROWHEAD_LENGTH = 10;
    public static final double CAUSAL_ARROWHEAD_WIDTH = 7;
    public static final Font CAUSAL_POLARITY_FONT = Font.font("System", FontWeight.BOLD, 12);

    // Module dimensions
    public static final double MODULE_WIDTH = 120;
    public static final double MODULE_HEIGHT = 70;
    public static final double MODULE_BORDER_WIDTH = 2.0;
    public static final double MODULE_CORNER_RADIUS = 6;

    // Flow indicator
    public static final double FLOW_INDICATOR_SIZE = 30;
    public static final double FLOW_HIT_HALF_WIDTH = 55;
    public static final double FLOW_HIT_HALF_HEIGHT = 35;
    /** Maximum pixel width for flow name labels before truncation. */
    public static final double FLOW_LABEL_MAX_WIDTH = 100;

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
    public static final double CLOUD_OFFSET = 80;
    public static final double CLOUD_LINE_WIDTH = 1.5;

    // Text positioning offsets
    /** Gap below flow diamond to name label. */
    public static final double FLOW_NAME_GAP = 4;
    /** Gap below flow diamond to equation label. */
    public static final double FLOW_EQUATION_GAP = 18;
    /** Offset from element center for name when a sub-label is present. */
    public static final double LABEL_NAME_OFFSET = -6;
    /** Offset from element center for sub-label (equation or value). */
    public static final double LABEL_SUBLABEL_OFFSET = 8;
    /** Minimum width for equation editor text fields. */
    public static final double EQUATION_EDITOR_MIN_WIDTH = 150;
    /** Y offset from flow center to equation editor position (world units). */
    public static final double FLOW_EQUATION_EDITOR_OFFSET = 28;

    // Fonts
    public static final Font STOCK_NAME_FONT = Font.font("System", FontWeight.BOLD, 13);
    public static final Font AUX_NAME_FONT = Font.font("System", FontWeight.NORMAL, 12);
    public static final Font MODULE_NAME_FONT = Font.font("System", FontWeight.BOLD, 13);
    public static final Font BADGE_FONT = Font.font("System", FontWeight.NORMAL, 9);
    public static final Font FLOW_NAME_FONT = Font.font("System", FontWeight.NORMAL, 11);

    /**
     * Returns the width for a given element type.
     */
    public static double widthFor(ElementType type) {
        return switch (type) {
            case STOCK -> STOCK_WIDTH;
            case AUX -> AUX_WIDTH;
            case MODULE -> MODULE_WIDTH;
            case LOOKUP -> LOOKUP_WIDTH;
            case FLOW -> FLOW_INDICATOR_SIZE;
            case CLD_VARIABLE -> CLD_VAR_WIDTH;
        };
    }

    /**
     * Returns the height for a given element type.
     */
    public static double heightFor(ElementType type) {
        return switch (type) {
            case STOCK -> STOCK_HEIGHT;
            case AUX -> AUX_HEIGHT;
            case MODULE -> MODULE_HEIGHT;
            case LOOKUP -> LOOKUP_HEIGHT;
            case FLOW -> FLOW_INDICATOR_SIZE;
            case CLD_VARIABLE -> CLD_VAR_HEIGHT;
        };
    }

    /**
     * Returns the effective width for the named element, using a custom size if set,
     * otherwise the default width for the element's type.
     */
    public static double effectiveWidth(CanvasState state, String name) {
        if (state.hasCustomSize(name)) {
            return state.getWidth(name);
        }
        return state.getType(name).map(LayoutMetrics::widthFor).orElse(AUX_WIDTH);
    }

    /**
     * Returns the effective height for the named element, using a custom size if set,
     * otherwise the default height for the element's type.
     */
    public static double effectiveHeight(CanvasState state, String name) {
        if (state.hasCustomSize(name)) {
            return state.getHeight(name);
        }
        return state.getType(name).map(LayoutMetrics::heightFor).orElse(AUX_HEIGHT);
    }

    /**
     * Returns the minimum width allowed when resizing an element of the given type.
     */
    public static double minWidthFor(ElementType type) {
        return switch (type) {
            case STOCK -> 80;
            case AUX -> 60;
            case MODULE -> 70;
            case LOOKUP -> 60;
            case CLD_VARIABLE -> 60;
            case FLOW -> 50;
        };
    }

    /**
     * Returns the minimum height allowed when resizing an element of the given type.
     */
    public static double minHeightFor(ElementType type) {
        return switch (type) {
            case STOCK -> 45;
            case AUX -> 35;
            case MODULE -> 45;
            case LOOKUP -> 35;
            case CLD_VARIABLE -> 25;
            case FLOW -> 30;
        };
    }

    /**
     * Computes the width for a CLD variable based on its text label.
     * Returns the measured text width plus padding, clamped to the minimum.
     */
    public static double cldVarWidthForName(String name) {
        Text text = new Text(name);
        text.setFont(AUX_NAME_FONT);
        double textWidth = text.getLayoutBounds().getWidth();
        return Math.max(minWidthFor(ElementType.CLD_VARIABLE), textWidth + CLD_VAR_TEXT_PADDING);
    }
}

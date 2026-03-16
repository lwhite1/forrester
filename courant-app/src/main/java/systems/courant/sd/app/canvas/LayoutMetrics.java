package systems.courant.sd.app.canvas;

import systems.courant.sd.model.def.CldVariableDef;
import systems.courant.sd.model.def.ElementType;
import systems.courant.sd.model.def.LookupTableDef;
import systems.courant.sd.model.def.ModelDefinition;
import systems.courant.sd.model.def.ModuleInstanceDef;
import systems.courant.sd.model.def.StockDef;
import systems.courant.sd.model.def.VariableDef;
import systems.courant.sd.model.graph.ElementSizes;

import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.util.HashMap;
import java.util.Map;

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

    // Comment dimensions
    public static final double COMMENT_WIDTH = 160;
    public static final double COMMENT_HEIGHT = 80;
    public static final double COMMENT_BORDER_WIDTH = 1;
    public static final double COMMENT_CORNER_RADIUS = 4;
    public static final double COMMENT_ACCENT_WIDTH = 4;
    public static final Font COMMENT_TEXT_FONT = Font.font("System", FontWeight.NORMAL, 11);

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
    /** Larger font for the "?" glyph on UNKNOWN polarity links. */
    public static final double CAUSAL_UNKNOWN_FONT_SIZE = 16;
    public static final Font CAUSAL_UNKNOWN_FONT = Font.font("System", FontWeight.BOLD, CAUSAL_UNKNOWN_FONT_SIZE);

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
    public static final double CLOUD_RADIUS = 16;
    public static final double CLOUD_OFFSET = 84;
    public static final double CLOUD_LINE_WIDTH = 2.5;
    public static final Font CLOUD_SYMBOL_FONT = Font.font("System", FontWeight.NORMAL, 14);

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

    // Font sizes (used by both Canvas renderer and SVG exporter)
    public static final double STOCK_NAME_FONT_SIZE = 13;
    public static final double AUX_NAME_FONT_SIZE = 12;
    public static final double MODULE_NAME_FONT_SIZE = 13;
    public static final double BADGE_FONT_SIZE = 10;
    public static final double FLOW_NAME_FONT_SIZE = 11;
    public static final double LOOKUP_NAME_FONT_SIZE = 11;
    public static final double COMMENT_TEXT_FONT_SIZE = 11;
    public static final double CAUSAL_POLARITY_FONT_SIZE = 12;

    // Feedback loop rendering
    public static final double LOOP_GLOW_PADDING = 6;
    public static final double LOOP_GLOW_LINE_WIDTH = 2.5;
    public static final double LOOP_EDGE_LINE_WIDTH = 2.5;
    public static final double LOOP_LABEL_FONT_SIZE = 14;
    public static final double LOOP_LABEL_PADDING = 6;

    // Fonts
    public static final Font STOCK_NAME_FONT = Font.font("System", FontWeight.BOLD, STOCK_NAME_FONT_SIZE);
    public static final Font AUX_NAME_FONT = Font.font("System", FontWeight.NORMAL, AUX_NAME_FONT_SIZE);
    public static final Font MODULE_NAME_FONT = Font.font("System", FontWeight.BOLD, MODULE_NAME_FONT_SIZE);
    public static final Font BADGE_FONT = Font.font("System", FontWeight.BOLD, BADGE_FONT_SIZE);
    public static final Font FLOW_NAME_FONT = Font.font("System", FontWeight.NORMAL, FLOW_NAME_FONT_SIZE);

    /** Font for the unit badge on stocks (centered below name). */
    public static final double UNIT_BADGE_FONT_SIZE = 10;
    public static final Font UNIT_BADGE_FONT = Font.font("System", FontWeight.NORMAL, UNIT_BADGE_FONT_SIZE);

    /** Maximum auto-computed width for AUX variables (2x default). */
    public static final double AUX_MAX_AUTO_WIDTH = 200;
    /** Maximum auto-computed width for LOOKUP elements (2x default). */
    public static final double LOOKUP_MAX_AUTO_WIDTH = 200;
    /** Maximum auto-computed width for STOCK elements (2x default). */
    public static final double STOCK_MAX_AUTO_WIDTH = 280;
    /** Maximum auto-computed width for MODULE elements (2x default). */
    public static final double MODULE_MAX_AUTO_WIDTH = 240;
    /** Horizontal padding around text for AUX variable auto-sizing. */
    public static final double AUX_TEXT_PADDING = 24;
    /** Horizontal padding around text for LOOKUP auto-sizing. */
    public static final double LOOKUP_TEXT_PADDING = 20;
    /** Horizontal padding around text for STOCK auto-sizing. */
    public static final double STOCK_TEXT_PADDING = 24;
    /** Horizontal padding around text for MODULE auto-sizing. */
    public static final double MODULE_TEXT_PADDING = 24;

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
            case COMMENT -> COMMENT_WIDTH;
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
            case COMMENT -> COMMENT_HEIGHT;
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
            case COMMENT -> 80;
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
            case COMMENT -> 40;
            case FLOW -> 30;
        };
    }

    /**
     * Computes the width for a STOCK based on its text label.
     * Returns the measured text width plus padding, clamped to
     * [{@link #STOCK_WIDTH}, {@link #STOCK_MAX_AUTO_WIDTH}].
     */
    public static double stockWidthForName(String name) {
        Text text = new Text(name);
        text.setFont(STOCK_NAME_FONT);
        double textWidth = text.getLayoutBounds().getWidth();
        return Math.max(STOCK_WIDTH,
                Math.min(textWidth + STOCK_TEXT_PADDING, STOCK_MAX_AUTO_WIDTH));
    }

    /**
     * Computes the width for a MODULE based on its text label.
     * Returns the measured text width plus padding, clamped to
     * [{@link #MODULE_WIDTH}, {@link #MODULE_MAX_AUTO_WIDTH}].
     */
    public static double moduleWidthForName(String name) {
        Text text = new Text(name);
        text.setFont(MODULE_NAME_FONT);
        double textWidth = text.getLayoutBounds().getWidth();
        return Math.max(MODULE_WIDTH,
                Math.min(textWidth + MODULE_TEXT_PADDING, MODULE_MAX_AUTO_WIDTH));
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

    /**
     * Computes the width for an AUX variable based on its text label.
     * Returns the measured text width plus padding, clamped to
     * [{@link #minWidthFor(ElementType) minWidth}, {@link #AUX_MAX_AUTO_WIDTH}].
     */
    public static double auxWidthForName(String name) {
        Text text = new Text(name);
        text.setFont(AUX_NAME_FONT);
        double textWidth = text.getLayoutBounds().getWidth();
        return Math.max(minWidthFor(ElementType.AUX),
                Math.min(textWidth + AUX_TEXT_PADDING, AUX_MAX_AUTO_WIDTH));
    }

    /**
     * Computes the width for a LOOKUP element based on its text label.
     * Returns the measured text width plus padding, clamped to
     * [{@link #minWidthFor(ElementType) minWidth}, {@link #LOOKUP_MAX_AUTO_WIDTH}].
     */
    public static double lookupWidthForName(String name) {
        Text text = new Text(name);
        text.setFont(LOOKUP_NAME_FONT);
        double textWidth = text.getLayoutBounds().getWidth();
        return Math.max(minWidthFor(ElementType.LOOKUP),
                Math.min(textWidth + LOOKUP_TEXT_PADDING, LOOKUP_MAX_AUTO_WIDTH));
    }

    /**
     * Computes per-element size overrides for elements whose rendered size depends on
     * their name text (AUX, LOOKUP, CLD_VARIABLE). These overrides should be passed to
     * {@link systems.courant.sd.model.graph.AutoLayout#layout(ModelDefinition, Map)}
     * so the layout algorithm uses actual rendered dimensions instead of fixed defaults.
     *
     * @param def the model definition whose elements need size computation
     * @return a map of element name to {@link ElementSizes} for elements with text-dependent widths
     */
    public static Map<String, ElementSizes> computeSizeOverrides(ModelDefinition def) {
        Map<String, ElementSizes> overrides = new HashMap<>();
        for (StockDef s : def.stocks()) {
            double w = stockWidthForName(s.name());
            overrides.put(s.name(), new ElementSizes(w, STOCK_HEIGHT));
        }
        for (ModuleInstanceDef m : def.modules()) {
            double w = moduleWidthForName(m.instanceName());
            overrides.put(m.instanceName(), new ElementSizes(w, MODULE_HEIGHT));
        }
        for (VariableDef v : def.variables()) {
            double w = auxWidthForName(v.name());
            overrides.put(v.name(), new ElementSizes(w, AUX_HEIGHT));
        }
        for (LookupTableDef t : def.lookupTables()) {
            double w = lookupWidthForName(t.name());
            overrides.put(t.name(), new ElementSizes(w, LOOKUP_HEIGHT));
        }
        for (CldVariableDef c : def.cldVariables()) {
            double w = cldVarWidthForName(c.name());
            overrides.put(c.name(), new ElementSizes(w, CLD_VAR_HEIGHT));
        }
        return overrides;
    }
}

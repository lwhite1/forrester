package systems.courant.sd.app.canvas;

import javafx.stage.Screen;

/**
 * Shared inline CSS style constants and layout utilities used across UI components.
 * Centralizes style strings to prevent duplication and simplify theme changes.
 */
public final class Styles {

    private Styles() {
    }

    /**
     * Standard width for configuration dialogs (SimulationSettings, ParameterSweep,
     * MultiParameterSweep, MonteCarlo, Optimizer).
     */
    public static final double CONFIG_DIALOG_WIDTH = 520;

    /**
     * Returns the given dimension clamped to 80% of the primary screen width.
     */
    public static double screenAwareWidth(double desired) {
        double screenWidth = Screen.getPrimary().getBounds().getWidth();
        return Math.min(desired, screenWidth * 0.8);
    }

    /**
     * Returns the given dimension clamped to 80% of the primary screen height.
     */
    public static double screenAwareHeight(double desired) {
        double screenHeight = Screen.getPrimary().getBounds().getHeight();
        return Math.min(desired, screenHeight * 0.8);
    }

    // --- Status bar ---
    public static final String STATUS_BAR_BACKGROUND =
            "-fx-background-color: #E8EAED; -fx-border-color: #BDC3C7; -fx-border-width: 1 0 0 0;";
    public static final String STATUS_LABEL =
            "-fx-font-size: 11px; -fx-text-fill: #555;";

    // --- Breadcrumb bar ---
    public static final String BREADCRUMB_LINK =
            "-fx-background-color: transparent; -fx-text-fill: #0066cc;"
                    + " -fx-cursor: hand; -fx-padding: 0 2 0 2; -fx-underline: true;";
    public static final String BREADCRUMB_CURRENT =
            "-fx-font-weight: bold; -fx-padding: 0 2 0 2;";
    public static final String MUTED_TEXT =
            "-fx-text-fill: #888;";

    // --- Properties panel / forms ---
    public static final String PLACEHOLDER_TEXT =
            "-fx-text-fill: #7F8C8D; -fx-font-style: italic;";
    public static final String FIELD_LABEL =
            "-fx-font-weight: bold; -fx-font-size: 11px;";
    public static final String SMALL_TEXT =
            "-fx-font-size: 11px;";

    // --- Help icons ---
    public static final String HELP_ICON =
            "-fx-text-fill: #8899AA; -fx-font-size: 13px; -fx-cursor: hand;";

    // --- Validation ---
    public static final String VALIDATION_ERROR =
            "-fx-text-fill: #E74C3C; -fx-font-size: 11;";
    public static final String VALIDATION_WARNING =
            "-fx-text-fill: #cc7700; -fx-font-weight: bold;";

    // --- Equation validation ---
    public static final String EQUATION_ERROR_BORDER =
            "-fx-border-color: #E74C3C; -fx-border-width: 1.5;";
    public static final String EQUATION_ERROR_LABEL =
            "-fx-text-fill: #E74C3C; -fx-font-size: 10px; -fx-wrap-text: true;";

    // --- Dimensional analysis ---
    public static final String DIMENSION_LABEL =
            "-fx-text-fill: #6C757D; -fx-font-size: 10px;";
    public static final String DIMENSION_MATCH =
            "-fx-text-fill: #27AE60; -fx-font-size: 10px;";
    public static final String DIMENSION_MISMATCH =
            "-fx-text-fill: #E67E22; -fx-font-size: 10px;";

    // --- Binding config dialog ---
    public static final String SECTION_HEADER =
            "-fx-font-weight: bold; -fx-font-size: 13px;";
    public static final String BOLD_TEXT =
            "-fx-font-weight: bold;";

    // --- Activity log panel ---
    public static final String ACTIVITY_LOG_PANEL =
            "-fx-background-color: #F5F6F8; -fx-border-color: #BDC3C7; -fx-border-width: 0 1 0 0;";
    public static final String ACTIVITY_LOG_TITLE =
            "-fx-font-weight: bold; -fx-font-size: 13px; -fx-padding: 8 8 4 8;";
    public static final String TRANSPARENT_BACKGROUND =
            "-fx-background-color: transparent;";
    public static final String LOG_TIME_LABEL =
            "-fx-font-size: 9px; -fx-text-fill: #999;";
    public static final String LOG_MESSAGE_LABEL =
            "-fx-font-size: 11px;";
}

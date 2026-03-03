package com.deathrayresearch.forrester.app.canvas;

/**
 * Shared inline CSS style constants used across UI components.
 * Centralizes style strings to prevent duplication and simplify theme changes.
 */
public final class Styles {

    private Styles() {
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

    // --- Binding config dialog ---
    public static final String SECTION_HEADER =
            "-fx-font-weight: bold; -fx-font-size: 13px;";
    public static final String BOLD_TEXT =
            "-fx-font-weight: bold;";
}

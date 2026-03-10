package systems.courant.forrester.app.canvas;

import javafx.scene.chart.XYChart;

import java.util.List;

/**
 * Shared chart utilities: color palette, series coloring, and number formatting.
 */
final class ChartUtils {

    private static final String[] SERIES_COLORS_ARRAY = {
        "#1f77b4", "#ff7f0e", "#2ca02c", "#d62728", "#9467bd",
        "#8c564b", "#e377c2", "#7f7f7f", "#bcbd22", "#17becf"
    };

    static final java.util.List<String> SERIES_COLORS = java.util.List.of(SERIES_COLORS_ARRAY);

    /** Muted colors for ghost run overlays, one per retained run (max 5). */
    static final java.util.List<String> GHOST_COLORS = java.util.List.of(
            "#4A90D9", "#2E7D32", "#E65100", "#7B1FA2", "#C62828"
    );

    /** Opacity applied to ghost run chart series. */
    static final double GHOST_OPACITY = 0.30;

    private ChartUtils() {
    }

    static void applySeriesColors(List<XYChart.Series<Number, Number>> allSeries) {
        for (int i = 0; i < allSeries.size(); i++) {
            String color = SERIES_COLORS.get(i % SERIES_COLORS.size());
            XYChart.Series<Number, Number> series = allSeries.get(i);
            series.nodeProperty().addListener((obs, oldNode, newNode) -> {
                if (newNode != null) {
                    newNode.setStyle("-fx-stroke: " + color + ";");
                }
            });
            if (series.getNode() != null) {
                series.getNode().setStyle("-fx-stroke: " + color + ";");
            }
        }
    }

    static String formatNumber(double value) {
        if (value == Math.floor(value) && Double.isFinite(value)
                && Math.abs(value) <= Long.MAX_VALUE) {
            return String.valueOf((long) value);
        }
        return String.format("%.4f", value);
    }
}

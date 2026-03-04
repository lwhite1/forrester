package com.deathrayresearch.forrester.app.canvas;

import javafx.scene.chart.XYChart;

import java.util.List;

/**
 * Shared chart utilities: color palette, series coloring, and number formatting.
 */
final class ChartUtils {

    static final String[] SERIES_COLORS = {
        "#1f77b4", "#ff7f0e", "#2ca02c", "#d62728", "#9467bd",
        "#8c564b", "#e377c2", "#7f7f7f", "#bcbd22", "#17becf"
    };

    private ChartUtils() {
    }

    static void applySeriesColors(List<XYChart.Series<Number, Number>> allSeries) {
        for (int i = 0; i < allSeries.size(); i++) {
            String color = SERIES_COLORS[i % SERIES_COLORS.length];
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
        if (value == Math.floor(value) && Double.isFinite(value)) {
            return String.valueOf((long) value);
        }
        return String.format("%.4f", value);
    }
}

package systems.courant.forrester.app.canvas;

import systems.courant.forrester.model.graph.FeedbackAnalysis;
import systems.courant.forrester.model.graph.LoopDominanceAnalysis;

import javafx.geometry.Insets;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;

import java.util.List;

/**
 * Displays a stacked area chart showing loop dominance over simulation time.
 * Each loop is a colored area; the tallest area at any time step represents
 * the dominant loop.
 */
final class LoopDominancePane extends VBox {

    LoopDominancePane(LoopDominanceAnalysis dominance) {
        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("Step");
        xAxis.setAutoRanging(true);

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Loop Activity");
        yAxis.setAutoRanging(true);

        AreaChart<Number, Number> chart = new AreaChart<>(xAxis, yAxis);
        chart.setTitle("Loop Dominance");
        chart.setAnimated(false);
        chart.setCreateSymbols(false);

        List<String> labels = dominance.loopLabels();
        for (int i = 0; i < dominance.loopCount(); i++) {
            XYChart.Series<Number, Number> series = new XYChart.Series<>();
            String type = dominance.loopTypes().get(i) != null
                    ? " (" + LoopNavigatorBar.formatType(dominance.loopTypes().get(i)) + ")"
                    : "";
            series.setName(labels.get(i) + type);

            // Sample every N steps if there are too many data points
            int totalSteps = dominance.stepCount();
            int sampleInterval = Math.max(1, totalSteps / 500);

            for (int step = 0; step < totalSteps; step += sampleInterval) {
                series.getData().add(new XYChart.Data<>(step, dominance.score(i, step)));
            }
            chart.getData().add(series);
        }

        // Apply loop colors
        applyLoopColors(chart, dominance);

        VBox.setVgrow(chart, Priority.ALWAYS);

        Label helpText = new Label(
                "This chart shows which feedback loop is driving the most change at each "
                + "time step. The tallest area at any point indicates the dominant loop. "
                + "Activity is measured as the sum of absolute stock-value changes for "
                + "each loop between consecutive steps. A shift in dominance often signals "
                + "a transition in system behavior \u2014 e.g., from exponential growth "
                + "(reinforcing) to goal-seeking (balancing).");
        helpText.setWrapText(true);
        helpText.setStyle("-fx-font-size: 11px; -fx-text-fill: #555; -fx-padding: 4 8 4 8;");

        TitledPane helpPane = new TitledPane("How to read this chart", helpText);
        helpPane.setExpanded(false);
        helpPane.setAnimated(false);
        helpPane.setPadding(new Insets(4, 0, 0, 0));

        getChildren().addAll(chart, helpPane);
    }

    private void applyLoopColors(AreaChart<Number, Number> chart,
                                 LoopDominanceAnalysis dominance) {
        String[] rColors = {"#27ae60", "#2ecc71", "#1abc9c", "#16a085"};
        String[] bColors = {"#2980b9", "#3498db", "#2c3e50", "#34495e"};
        String[] neutralColors = {"#7f8c8d", "#95a5a6", "#bdc3c7"};

        for (int i = 0; i < dominance.loopCount(); i++) {
            FeedbackAnalysis.LoopType type = dominance.loopTypes().get(i);
            String color;
            if (type == FeedbackAnalysis.LoopType.REINFORCING) {
                color = rColors[i % rColors.length];
            } else if (type == FeedbackAnalysis.LoopType.BALANCING) {
                color = bColors[i % bColors.length];
            } else {
                color = neutralColors[i % neutralColors.length];
            }

            if (i < chart.getData().size()) {
                XYChart.Series<Number, Number> series = chart.getData().get(i);
                if (series.getNode() != null) {
                    series.getNode().setStyle("-fx-fill: " + color + "40;");
                }
                for (XYChart.Data<Number, Number> d : series.getData()) {
                    if (d.getNode() != null) {
                        d.getNode().setVisible(false);
                    }
                }
            }
        }
    }
}

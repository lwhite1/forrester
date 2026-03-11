package systems.courant.forrester.app.canvas;

import systems.courant.forrester.sweep.SensitivitySummary;
import systems.courant.forrester.sweep.SensitivitySummary.ParameterImpact;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiFunction;

/**
 * Displays a sensitivity analysis as a tornado chart (horizontal bar chart ranked
 * by impact magnitude) and a plain-language summary below it.
 */
public class SensitivityPane extends BorderPane {

    private static final String NEGATIVE_COLOR = "#4393c3";
    private static final String POSITIVE_COLOR = "#d6604d";

    private final List<String> trackableNames;
    private final BiFunction<String, Void, List<ParameterImpact>> impactComputer;

    /**
     * Creates a sensitivity pane driven by a pre-computed impact function.
     *
     * @param trackableNames the stock and variable names available for analysis
     * @param impactComputer computes impacts for a given target variable name
     * @param initialVariable the variable to display initially
     */
    public SensitivityPane(List<String> trackableNames,
                           BiFunction<String, Void, List<ParameterImpact>> impactComputer,
                           String initialVariable) {
        this.trackableNames = trackableNames;
        this.impactComputer = impactComputer;

        ComboBox<String> varCombo = new ComboBox<>(
                FXCollections.observableArrayList(trackableNames));
        varCombo.setValue(initialVariable);

        HBox topBar = new HBox(8, new Label("Target variable:"), varCombo);
        topBar.setPadding(new Insets(8));
        setTop(topBar);

        varCombo.valueProperty().addListener((obs, old, val) -> {
            if (val != null) {
                showSensitivity(val);
            }
        });

        showSensitivity(initialVariable);
    }

    private void showSensitivity(String targetVariable) {
        List<ParameterImpact> impacts = impactComputer.apply(targetVariable, null);

        VBox content = new VBox(12);
        content.setPadding(new Insets(12));

        if (impacts.isEmpty()) {
            content.getChildren().add(new Label("No sensitivity data available."));
        } else {
            content.getChildren().add(buildTornadoChart(impacts));
            content.getChildren().add(buildSummaryText(impacts));
        }

        setCenter(content);
    }

    private BarChart<Number, String> buildTornadoChart(List<ParameterImpact> impacts) {
        NumberAxis xAxis = new NumberAxis(0, 100, 10);
        xAxis.setLabel("Variance explained (%)");

        CategoryAxis yAxis = new CategoryAxis();
        yAxis.setLabel("Parameter");

        BarChart<Number, String> chart = new BarChart<>(xAxis, yAxis);
        chart.setTitle("Sensitivity — Variance Decomposition");
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.setCategoryGap(4);
        chart.setBarGap(1);

        XYChart.Series<Number, String> series = new XYChart.Series<>();

        // Impacts are already sorted descending by magnitude.
        // Add in reverse so the most impactful parameter is at the top of the chart.
        List<String> categoryOrder = new ArrayList<>();
        for (int i = impacts.size() - 1; i >= 0; i--) {
            ParameterImpact impact = impacts.get(i);
            double pct = impact.impactFraction() * 100.0;
            String label = String.format(Locale.US, "%s (%.0f%%)",
                    impact.parameterName(), pct);
            categoryOrder.add(label);
            series.getData().add(new XYChart.Data<>(pct, label));
        }

        yAxis.setCategories(FXCollections.observableArrayList(categoryOrder));
        chart.getData().add(series);
        chart.setPrefHeight(Math.max(200, impacts.size() * 40 + 80));

        // Apply bar color after rendering
        String barColor = "#4393c3";
        chart.lookupAll(".chart-bar").forEach(node ->
                node.setStyle("-fx-bar-fill: " + barColor + ";"));
        chart.needsLayoutProperty().addListener((obs, old, val) ->
                chart.lookupAll(".chart-bar").forEach(node ->
                        node.setStyle("-fx-bar-fill: " + barColor + ";")));

        return chart;
    }

    private TextFlow buildSummaryText(List<ParameterImpact> impacts) {
        String summary = SensitivitySummary.toPlainLanguage(impacts);
        Text text = new Text(summary);
        text.setStyle("-fx-font-size: 13px;");
        TextFlow flow = new TextFlow(text);
        flow.setPadding(new Insets(8, 0, 0, 0));
        return flow;
    }
}

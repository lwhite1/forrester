package com.deathrayresearch.forrester.app.canvas;

import com.deathrayresearch.forrester.sweep.SensitivitySummary;
import com.deathrayresearch.forrester.sweep.SensitivitySummary.ParameterImpact;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Side;
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
        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("Impact (%)");

        CategoryAxis yAxis = new CategoryAxis();
        yAxis.setLabel("Parameter");

        BarChart<Number, String> chart = new BarChart<>(xAxis, yAxis);
        chart.setTitle("Sensitivity — Tornado Chart");
        chart.setLegendSide(Side.BOTTOM);
        chart.setAnimated(false);
        chart.setCategoryGap(4);
        chart.setBarGap(1);

        XYChart.Series<Number, String> negativeSeries = new XYChart.Series<>();
        negativeSeries.setName("Decrease");
        XYChart.Series<Number, String> positiveSeries = new XYChart.Series<>();
        positiveSeries.setName("Increase");

        // Impacts are already sorted descending by magnitude.
        // Add in reverse so the most impactful parameter is at the top of the chart.
        List<String> categoryOrder = new ArrayList<>();
        for (int i = impacts.size() - 1; i >= 0; i--) {
            ParameterImpact impact = impacts.get(i);
            double pct = Math.abs(impact.impactFraction()) * 100.0;
            String label = impact.parameterName();
            categoryOrder.add(label);

            negativeSeries.getData().add(new XYChart.Data<>(-pct, label));
            positiveSeries.getData().add(new XYChart.Data<>(pct, label));
        }

        yAxis.setCategories(FXCollections.observableArrayList(categoryOrder));

        chart.getData().add(negativeSeries);
        chart.getData().add(positiveSeries);

        chart.setPrefHeight(Math.max(200, impacts.size() * 40 + 80));

        // Apply colors after rendering
        chart.lookupAll(".default-color0.chart-bar").forEach(node ->
                node.setStyle("-fx-bar-fill: " + NEGATIVE_COLOR + ";"));
        chart.lookupAll(".default-color1.chart-bar").forEach(node ->
                node.setStyle("-fx-bar-fill: " + POSITIVE_COLOR + ";"));

        // Re-apply on layout to catch late CSS
        chart.needsLayoutProperty().addListener((obs, old, val) -> {
            chart.lookupAll(".default-color0.chart-bar").forEach(node ->
                    node.setStyle("-fx-bar-fill: " + NEGATIVE_COLOR + ";"));
            chart.lookupAll(".default-color1.chart-bar").forEach(node ->
                    node.setStyle("-fx-bar-fill: " + POSITIVE_COLOR + ";"));
        });

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

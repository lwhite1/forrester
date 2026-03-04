package com.deathrayresearch.forrester.app.canvas;

import com.deathrayresearch.forrester.sweep.MultiSweepResult;
import com.deathrayresearch.forrester.sweep.RunResult;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Displays multi-parameter sweep results with a Summary table tab and a
 * Time Series chart tab. The summary shows one row per parameter combination
 * with final and max stock values. The time series tab lets the user select
 * a specific run to chart.
 */
public class MultiSweepResultPane extends BorderPane {

    private final MultiSweepResult result;

    public MultiSweepResultPane(MultiSweepResult result) {
        this.result = result;

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab summaryTab = new Tab("Summary", buildSummaryTable());
        Tab timeSeriesTab = new Tab("Time Series", buildTimeSeriesPane());
        tabPane.getTabs().addAll(summaryTab, timeSeriesTab);

        setCenter(tabPane);
    }

    private TableView<RunResult> buildSummaryTable() {
        TableView<RunResult> table = new TableView<>();

        List<String> paramNames = result.getParameterNames();
        for (String paramName : paramNames) {
            TableColumn<RunResult, String> col = new TableColumn<>(paramName);
            col.setCellValueFactory(data -> {
                Double val = data.getValue().getParameterMap().get(paramName);
                return new SimpleStringProperty(val != null ? ChartUtils.formatNumber(val) : "");
            });
            col.setPrefWidth(100);
            table.getColumns().add(col);
        }

        List<String> stockNames = result.getStockNames();
        for (String stockName : stockNames) {
            TableColumn<RunResult, String> finalCol = new TableColumn<>(stockName + "_final");
            finalCol.setCellValueFactory(data -> {
                double val = data.getValue().getFinalStockValue(stockName);
                return new SimpleStringProperty(ChartUtils.formatNumber(val));
            });
            finalCol.setPrefWidth(120);
            table.getColumns().add(finalCol);

            TableColumn<RunResult, String> maxCol = new TableColumn<>(stockName + "_max");
            maxCol.setCellValueFactory(data -> {
                double val = data.getValue().getMaxStockValue(stockName);
                return new SimpleStringProperty(ChartUtils.formatNumber(val));
            });
            maxCol.setPrefWidth(120);
            table.getColumns().add(maxCol);
        }

        table.setItems(FXCollections.observableArrayList(result.getResults()));

        ContextMenu contextMenu = new ContextMenu();
        MenuItem exportSummary = new MenuItem("Export CSV (Summary)...");
        exportSummary.setOnAction(e -> exportSummaryCsv());
        contextMenu.getItems().add(exportSummary);
        table.setContextMenu(contextMenu);

        return table;
    }

    private BorderPane buildTimeSeriesPane() {
        BorderPane pane = new BorderPane();

        List<String> runLabels = new ArrayList<>();
        for (int i = 0; i < result.getRunCount(); i++) {
            RunResult run = result.getResult(i);
            String label = run.getParameterMap().entrySet().stream()
                    .map(entry -> entry.getKey() + "=" + ChartUtils.formatNumber(entry.getValue()))
                    .collect(Collectors.joining(", "));
            runLabels.add(label);
        }

        ComboBox<String> runCombo = new ComboBox<>(FXCollections.observableArrayList(runLabels));

        HBox topBar = new HBox(8, new Label("Run:"), runCombo);
        topBar.setPadding(new Insets(8));
        pane.setTop(topBar);

        runCombo.valueProperty().addListener((obs, old, val) -> {
            if (val != null) {
                int index = runLabels.indexOf(val);
                if (index >= 0) {
                    pane.setCenter(buildRunChart(result.getResult(index)));
                }
            }
        });

        if (!runLabels.isEmpty()) {
            runCombo.setValue(runLabels.get(0));
        }

        return pane;
    }

    private LineChart<Number, Number> buildRunChart(RunResult run) {
        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("Step");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Value");

        LineChart<Number, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setCreateSymbols(false);
        chart.setAnimated(false);

        List<String> allNames = new ArrayList<>();
        allNames.addAll(run.getStockNames());
        allNames.addAll(run.getVariableNames());

        List<XYChart.Series<Number, Number>> allSeries = new ArrayList<>();

        for (int c = 0; c < allNames.size(); c++) {
            boolean isStock = c < run.getStockNames().size();
            int colIndex = isStock ? c : c - run.getStockNames().size();

            XYChart.Series<Number, Number> series = new XYChart.Series<>();
            series.setName(allNames.get(c));

            for (int s = 0; s < run.getStepCount(); s++) {
                double value = isStock
                        ? run.getStockValuesAtStep(s).get(colIndex)
                        : run.getVariableValuesAtStep(s).get(colIndex);
                series.getData().add(new XYChart.Data<>(run.getStep(s), value));
            }
            allSeries.add(series);
        }

        chart.getData().addAll(allSeries);
        ChartUtils.applySeriesColors(allSeries);

        ContextMenu contextMenu = new ContextMenu();
        MenuItem exportTs = new MenuItem("Export CSV (Time Series)...");
        exportTs.setOnAction(e -> exportTimeSeriesCsv());
        contextMenu.getItems().add(exportTs);
        chart.setOnContextMenuRequested(e ->
                contextMenu.show(chart, e.getScreenX(), e.getScreenY()));

        return chart;
    }

    private void exportSummaryCsv() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Summary CSV");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        fileChooser.setInitialFileName("multi_sweep_summary.csv");

        File file = fileChooser.showSaveDialog(getScene() != null ? getScene().getWindow() : null);
        if (file != null) {
            result.writeSummaryCsv(file.getAbsolutePath());
        }
    }

    private void exportTimeSeriesCsv() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Time Series CSV");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        fileChooser.setInitialFileName("multi_sweep_timeseries.csv");

        File file = fileChooser.showSaveDialog(getScene() != null ? getScene().getWindow() : null);
        if (file != null) {
            result.writeTimeSeriesCsv(file.getAbsolutePath());
        }
    }
}

package systems.courant.sd.app.canvas.charts;

import systems.courant.sd.sweep.RunResult;
import systems.courant.sd.sweep.SweepResult;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import java.util.ArrayList;
import java.util.List;
import systems.courant.sd.app.canvas.ChartUtils;
import systems.courant.sd.app.canvas.ClipboardExporter;

/**
 * Displays parameter sweep results as a line chart with one series per
 * parameter value. Includes a ComboBox to select which stock/variable
 * to plot and a checkbox sidebar for toggling series visibility.
 */
public class SweepResultPane extends BorderPane {

    private final SweepResult result;
    private final String paramName;
    private LineChart<Number, Number> currentChart;

    public SweepResultPane(SweepResult result, String paramName) {
        this.result = result;
        this.paramName = paramName;

        List<String> trackableNames = new ArrayList<>();
        trackableNames.addAll(result.getStockNames());
        trackableNames.addAll(result.getVariableNames());

        ComboBox<String> varCombo = new ComboBox<>(FXCollections.observableArrayList(trackableNames));
        if (!trackableNames.isEmpty()) {
            varCombo.setValue(trackableNames.getFirst());
        }

        HBox topBar = new HBox(8, new Label("Variable:"), varCombo);
        topBar.setPadding(new Insets(8));
        setTop(topBar);

        varCombo.valueProperty().addListener((obs, old, val) -> {
            if (val != null) {
                buildChart(val);
            }
        });

        if (!trackableNames.isEmpty()) {
            buildChart(trackableNames.getFirst());
        }
    }

    private void buildChart(String variableName) {
        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("Step");

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel(variableName);

        LineChart<Number, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setCreateSymbols(false);
        chart.setAnimated(false);
        chart.setLegendVisible(false);

        List<XYChart.Series<Number, Number>> allSeries = new ArrayList<>();
        boolean isStock = result.getStockNames().contains(variableName);
        int colIndex = isStock
                ? result.getStockNames().indexOf(variableName)
                : result.getVariableNames().indexOf(variableName);

        if (colIndex < 0) {
            return;
        }

        for (int r = 0; r < result.getRunCount(); r++) {
            RunResult run = result.getResult(r);
            double paramValue = run.getParameterValue();

            XYChart.Series<Number, Number> series = new XYChart.Series<>();
            series.setName(paramName + " = " + ChartUtils.formatNumber(paramValue));

            for (int s = 0; s < run.getStepCount(); s++) {
                double value = isStock
                        ? run.getStockValuesAtStep(s)[colIndex]
                        : run.getVariableValuesAtStep(s)[colIndex];
                series.getData().add(new XYChart.Data<>(run.getStep(s), value));
            }
            allSeries.add(series);
        }

        chart.getData().addAll(allSeries);
        ChartUtils.applySeriesColors(allSeries);
        this.currentChart = chart;

        VBox sidebar = new VBox(6);
        sidebar.setPadding(new Insets(10));

        for (int i = 0; i < allSeries.size(); i++) {
            XYChart.Series<Number, Number> series = allSeries.get(i);
            String color = ChartUtils.SERIES_COLORS.get(i % ChartUtils.SERIES_COLORS.size());

            CheckBox cb = new CheckBox(series.getName());
            cb.setSelected(true);
            cb.setStyle("-fx-text-fill: " + color + ";");
            cb.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
                if (series.getNode() != null) {
                    series.getNode().setVisible(isSelected);
                }
                series.getData().forEach(d -> {
                    if (d.getNode() != null) {
                        d.getNode().setVisible(isSelected);
                    }
                });
            });
            sidebar.getChildren().add(cb);
        }

        ScrollPane sidebarScroll = new ScrollPane(sidebar);
        sidebarScroll.setFitToWidth(true);
        sidebarScroll.setPrefWidth(180);

        ContextMenu contextMenu = new ContextMenu();
        MenuItem saveItem = new MenuItem("Save as PNG...");
        saveItem.setOnAction(e -> saveChartAsPng());
        MenuItem exportTs = new MenuItem("Export CSV (Time Series)...");
        exportTs.setOnAction(e -> exportTimeSeriesCsv());
        MenuItem exportSummary = new MenuItem("Export CSV (Summary)...");
        exportSummary.setOnAction(e -> exportSummaryCsv());
        MenuItem copyTs = new MenuItem("Copy to Clipboard (Time Series)");
        copyTs.setOnAction(e -> ClipboardExporter.copySweepTimeSeries(result));
        MenuItem copySummary = new MenuItem("Copy to Clipboard (Summary)");
        copySummary.setOnAction(e -> ClipboardExporter.copySweepSummary(result));
        contextMenu.getItems().addAll(saveItem, exportTs, exportSummary, copyTs, copySummary);
        chart.setOnContextMenuRequested(e ->
                contextMenu.show(chart, e.getScreenX(), e.getScreenY()));

        setCenter(chart);
        setRight(sidebarScroll);
    }

    private void exportTimeSeriesCsv() {
        ChartUtils.showCsvSaveDialog("Export Time Series CSV", "sweep_timeseries.csv",
                getScene() != null ? getScene().getWindow() : null,
                file -> result.writeTimeSeriesCsv(file.getAbsolutePath()));
    }

    private void saveChartAsPng() {
        ChartUtils.saveNodeAsPng(currentChart, "sweep_chart.png",
                getScene() != null ? getScene().getWindow() : null);
    }

    private void exportSummaryCsv() {
        ChartUtils.showCsvSaveDialog("Export Summary CSV", "sweep_summary.csv",
                getScene() != null ? getScene().getWindow() : null,
                file -> result.writeSummaryCsv(file.getAbsolutePath()));
    }
}

package systems.courant.sd.app.canvas.charts;

import systems.courant.sd.app.canvas.dialogs.CalibrateDialog;
import systems.courant.sd.sweep.OptimizationResult;
import systems.courant.sd.sweep.RunResult;

import javafx.geometry.Insets;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Map;

import systems.courant.sd.app.canvas.ChartUtils;
import systems.courant.sd.app.canvas.ClipboardExporter;
import systems.courant.sd.app.canvas.CsvExportHelper;
import systems.courant.sd.app.canvas.Styles;

/**
 * Displays calibration results: a summary grid showing recovered parameters
 * and SSE, plus a line chart overlaying simulated vs observed data for each
 * fit target.
 */
public class CalibrationResultPane extends BorderPane {

    private final OptimizationResult result;
    private LineChart<Number, Number> chart;

    public CalibrationResultPane(OptimizationResult result,
                                  List<CalibrateDialog.FitTarget> fitTargets) {
        this.result = result;

        // Summary grid
        GridPane summaryGrid = new GridPane();
        summaryGrid.setHgap(10);
        summaryGrid.setVgap(4);
        summaryGrid.setPadding(new Insets(10));

        int row = 0;
        Label headerLabel = new Label("Calibration Results");
        headerLabel.setStyle(Styles.SECTION_HEADER);
        summaryGrid.add(headerLabel, 0, row++, 2, 1);

        summaryGrid.add(boldLabel("SSE:"), 0, row);
        summaryGrid.add(new Label(ChartUtils.formatNumber(result.getBestObjectiveValue())), 1, row++);

        summaryGrid.add(boldLabel("Evaluations:"), 0, row);
        summaryGrid.add(new Label(String.valueOf(result.getEvaluationCount())), 1, row++);

        for (Map.Entry<String, Double> entry : result.getBestParameters().entrySet()) {
            summaryGrid.add(boldLabel(entry.getKey() + ":"), 0, row);
            summaryGrid.add(new Label(ChartUtils.formatNumber(entry.getValue())), 1, row++);
        }

        // Chart overlaying simulated vs observed
        RunResult bestRun = result.getBestRunResult();
        chart = buildChart(bestRun, fitTargets);

        ContextMenu contextMenu = new ContextMenu();
        MenuItem saveItem = new MenuItem("Save as PNG...");
        saveItem.setOnAction(e -> saveChartAsPng());
        MenuItem exportCsv = new MenuItem("Export CSV (Best Run)...");
        exportCsv.setOnAction(e -> exportBestRunCsv());
        MenuItem copyItem = new MenuItem("Copy to Clipboard (Best Run)");
        copyItem.setOnAction(e -> ClipboardExporter.copyCalibrationBestRun(result));
        contextMenu.getItems().addAll(saveItem, exportCsv, copyItem);
        chart.setOnContextMenuRequested(e ->
                contextMenu.show(chart, e.getScreenX(), e.getScreenY()));

        VBox topSection = new VBox(summaryGrid);
        setTop(topSection);
        setCenter(chart);
    }

    private LineChart<Number, Number> buildChart(RunResult bestRun,
                                                  List<CalibrateDialog.FitTarget> fitTargets) {
        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("Step");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Value");

        LineChart<Number, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setCreateSymbols(false);
        lineChart.setAnimated(false);

        int colorIndex = 0;
        List<String> colors = ChartUtils.SERIES_COLORS;

        for (CalibrateDialog.FitTarget target : fitTargets) {
            String color = colors.get(colorIndex % colors.size());

            // Simulated series
            double[] simulated = bestRun.getStockSeries(target.stockName());
            XYChart.Series<Number, Number> simSeries = new XYChart.Series<>();
            simSeries.setName(target.stockName() + " (simulated)");
            for (int s = 0; s < simulated.length; s++) {
                simSeries.getData().add(new XYChart.Data<>(bestRun.getStep(s), simulated[s]));
            }
            lineChart.getData().add(simSeries);
            applyLineStyle(simSeries, color, false);

            // Observed series
            double[] observed = target.observedData();
            XYChart.Series<Number, Number> obsSeries = new XYChart.Series<>();
            obsSeries.setName(target.csvColumnName() + " (observed)");
            int obsLen = Math.min(observed.length, bestRun.getStepCount());
            for (int s = 0; s < obsLen; s++) {
                obsSeries.getData().add(new XYChart.Data<>(bestRun.getStep(s), observed[s]));
            }
            lineChart.getData().add(obsSeries);
            applyLineStyle(obsSeries, color, true);

            colorIndex++;
        }

        return lineChart;
    }

    private static void applyLineStyle(XYChart.Series<Number, Number> series,
                                        String color, boolean dashed) {
        String style = "-fx-stroke: " + color + ";"
                + (dashed ? " -fx-stroke-dash-array: 5 5;" : "");
        series.nodeProperty().addListener((obs, oldNode, newNode) -> {
            if (newNode != null) {
                newNode.setStyle(style);
            }
        });
        if (series.getNode() != null) {
            series.getNode().setStyle(style);
        }
    }

    private void saveChartAsPng() {
        ChartUtils.saveNodeAsPng(chart, "calibration_chart.png",
                getScene() != null ? getScene().getWindow() : null);
    }

    private void exportBestRunCsv() {
        ChartUtils.showCsvSaveDialog("Export Best Run CSV", "calibration_best_run.csv",
                getScene() != null ? getScene().getWindow() : null,
                file -> CsvExportHelper.writeRunResult(file, result.getBestRunResult()));
    }

    private static Label boldLabel(String text) {
        Label label = new Label(text);
        label.setStyle(Styles.BOLD_TEXT);
        return label;
    }
}

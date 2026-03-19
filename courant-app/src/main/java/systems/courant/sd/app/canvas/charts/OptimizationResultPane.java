package systems.courant.sd.app.canvas.charts;

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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import systems.courant.sd.app.canvas.ChartUtils;
import systems.courant.sd.app.canvas.ClipboardExporter;
import systems.courant.sd.app.canvas.CsvExportHelper;
import systems.courant.sd.app.canvas.Styles;

/**
 * Displays optimization results: a summary grid showing the best parameters,
 * objective value, and evaluation count, plus a line chart of the best-run
 * time series.
 */
public class OptimizationResultPane extends BorderPane {

    private final OptimizationResult result;
    private LineChart<Number, Number> chart;

    public OptimizationResultPane(OptimizationResult result) {
        this.result = result;
        // Summary grid
        GridPane summaryGrid = new GridPane();
        summaryGrid.setHgap(10);
        summaryGrid.setVgap(4);
        summaryGrid.setPadding(new Insets(10));

        int row = 0;
        Label headerLabel = new Label("Optimization Results");
        headerLabel.setStyle(Styles.SECTION_HEADER);
        summaryGrid.add(headerLabel, 0, row++, 2, 1);

        summaryGrid.add(boldLabel("Objective Value:"), 0, row);
        summaryGrid.add(new Label(ChartUtils.formatNumber(result.getBestObjectiveValue())), 1, row++);

        summaryGrid.add(boldLabel("Evaluations:"), 0, row);
        summaryGrid.add(new Label(String.valueOf(result.getEvaluationCount())), 1, row++);

        for (Map.Entry<String, Double> entry : result.getBestParameters().entrySet()) {
            summaryGrid.add(boldLabel(entry.getKey() + ":"), 0, row);
            summaryGrid.add(new Label(ChartUtils.formatNumber(entry.getValue())), 1, row++);
        }

        // Best-run time series chart
        RunResult bestRun = result.getBestRunResult();
        chart = buildChart(bestRun);

        ContextMenu contextMenu = new ContextMenu();
        MenuItem saveItem = new MenuItem("Save as PNG...");
        saveItem.setOnAction(e -> saveChartAsPng());
        MenuItem exportCsv = new MenuItem("Export CSV (Best Run)...");
        exportCsv.setOnAction(e -> exportBestRunCsv());
        MenuItem copyItem = new MenuItem("Copy to Clipboard (Best Run)");
        copyItem.setOnAction(e -> ClipboardExporter.copyOptimizationBestRun(result));
        contextMenu.getItems().addAll(saveItem, exportCsv, copyItem);
        chart.setOnContextMenuRequested(e ->
                contextMenu.show(chart, e.getScreenX(), e.getScreenY()));

        VBox topSection = new VBox(summaryGrid);
        setTop(topSection);
        setCenter(chart);
    }

    private LineChart<Number, Number> buildChart(RunResult bestRun) {
        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("Step");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Value");

        LineChart<Number, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setCreateSymbols(false);
        chart.setAnimated(false);

        List<String> allNames = new ArrayList<>();
        allNames.addAll(bestRun.getStockNames());
        allNames.addAll(bestRun.getVariableNames());

        for (int c = 0; c < allNames.size(); c++) {
            String name = allNames.get(c);
            if (ChartUtils.isSimulationSetting(name)) {
                continue;
            }
            boolean isStock = c < bestRun.getStockNames().size();
            int colIndex = isStock ? c : c - bestRun.getStockNames().size();

            XYChart.Series<Number, Number> series = new XYChart.Series<>();
            series.setName(name);

            for (int s = 0; s < bestRun.getStepCount(); s++) {
                double value = isStock
                        ? bestRun.getStockValuesAtStep(s)[colIndex]
                        : bestRun.getVariableValuesAtStep(s)[colIndex];
                series.getData().add(new XYChart.Data<>(bestRun.getStep(s), value));
            }
            chart.getData().add(series);
        }

        ChartUtils.applySeriesColors(chart.getData());

        return chart;
    }

    private void saveChartAsPng() {
        ChartUtils.saveNodeAsPng(chart, "optimization_chart.png",
                getScene() != null ? getScene().getWindow() : null);
    }

    private void exportBestRunCsv() {
        ChartUtils.showCsvSaveDialog("Export Best Run CSV", "optimization_best_run.csv",
                getScene() != null ? getScene().getWindow() : null,
                file -> CsvExportHelper.writeRunResult(file, result.getBestRunResult()));
    }

    private static Label boldLabel(String text) {
        Label label = new Label(text);
        label.setStyle(Styles.BOLD_TEXT);
        return label;
    }

}

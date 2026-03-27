package systems.courant.sd.app.canvas.charts;

import systems.courant.sd.sweep.RunResult;
import systems.courant.sd.sweep.SweepResult;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
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
    private final String timeStepLabel;
    public SweepResultPane(SweepResult result, String paramName, String timeStepLabel) {
        this.result = result;
        this.paramName = paramName;
        this.timeStepLabel = timeStepLabel;

        List<String> trackableNames = new ArrayList<>();
        trackableNames.addAll(ChartUtils.filterSimulationSettings(result.getStockNames()));
        trackableNames.addAll(ChartUtils.filterSimulationSettings(result.getVariableNames()));

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
        LineChart<Number, Number> chart = ChartUtils.createLineChart(timeStepLabel, variableName);

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
                double[] values = isStock
                        ? run.getStockValuesAtStep(s)
                        : run.getVariableValuesAtStep(s);
                if (colIndex >= values.length) {
                    break;
                }
                series.getData().add(new XYChart.Data<>(run.getStep(s), values[colIndex]));
            }
            allSeries.add(series);
        }

        chart.getData().addAll(allSeries);
        ChartUtils.applySeriesColors(allSeries);
        MenuItem copyTs = new MenuItem("Copy to Clipboard (Time Series)");
        copyTs.setOnAction(e -> ClipboardExporter.copySweepTimeSeries(result));
        MenuItem copySummary = new MenuItem("Copy to Clipboard (Summary)");
        copySummary.setOnAction(e -> ClipboardExporter.copySweepSummary(result));

        ChartUtils.attachContextMenu(chart,
                ChartUtils.createPngMenuItem(chart, "sweep_chart.png", this::getOwnerWindow),
                ChartUtils.createCsvMenuItem("Export CSV (Time Series)...",
                        "sweep_timeseries.csv", this::getOwnerWindow,
                        file -> result.writeTimeSeriesCsv(file.getAbsolutePath())),
                ChartUtils.createCsvMenuItem("Export CSV (Summary)...",
                        "sweep_summary.csv", this::getOwnerWindow,
                        file -> result.writeSummaryCsv(file.getAbsolutePath())),
                copyTs, copySummary);

        setCenter(chart);
        setRight(ChartUtils.buildSeriesToggleSidebar(allSeries));
    }

    private javafx.stage.Window getOwnerWindow() {
        return getScene() != null ? getScene().getWindow() : null;
    }
}

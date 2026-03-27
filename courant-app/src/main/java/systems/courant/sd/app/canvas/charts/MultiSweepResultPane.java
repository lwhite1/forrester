package systems.courant.sd.app.canvas.charts;

import systems.courant.sd.sweep.MultiSweepResult;
import systems.courant.sd.sweep.RunResult;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.util.StringConverter;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Window;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import systems.courant.sd.app.canvas.ChartUtils;
import systems.courant.sd.app.canvas.ClipboardExporter;

/**
 * Displays multi-parameter sweep results with a Summary table tab and a
 * Time Series chart tab. The summary shows one row per parameter combination
 * with final and max stock values. The time series tab lets the user select
 * a specific run to chart.
 */
public class MultiSweepResultPane extends BorderPane {

    private final MultiSweepResult result;
    private final String timeStepLabel;
    public MultiSweepResultPane(MultiSweepResult result, String timeStepLabel) {
        this.result = result;
        this.timeStepLabel = timeStepLabel;

        TabPane tabPane = new TabPane();
        tabPane.setId("multiSweepTabs");
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab summaryTab = new Tab("Summary", buildSummaryTable());
        Tab timeSeriesTab = new Tab("Time Series", buildTimeSeriesPane());
        tabPane.getTabs().addAll(summaryTab, timeSeriesTab);

        setCenter(tabPane);
    }

    private TableView<RunResult> buildSummaryTable() {
        TableView<RunResult> table = new TableView<>();
        table.setId("multiSweepSummaryTable");

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

        List<String> stockNames = ChartUtils.filterSimulationSettings(result.getStockNames());
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

        MenuItem exportSummary = ChartUtils.createCsvMenuItem("Export CSV (Summary)...",
                "multi_sweep_summary.csv", this::getOwnerWindow,
                file -> result.writeSummaryCsv(file.getAbsolutePath()));
        MenuItem copySummary = new MenuItem("Copy to Clipboard (Summary)");
        copySummary.setOnAction(e -> ClipboardExporter.copyMultiSweepSummary(result));
        ChartUtils.attachContextMenu(table, exportSummary, copySummary);

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

        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < result.getRunCount(); i++) {
            indices.add(i);
        }

        ComboBox<Integer> runCombo = new ComboBox<>(FXCollections.observableArrayList(indices));
        runCombo.setId("multiSweepRunCombo");
        runCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(Integer index) {
                return index != null && index < runLabels.size() ? runLabels.get(index) : "";
            }

            @Override
            public Integer fromString(String string) {
                return null;
            }
        });

        HBox topBar = new HBox(8, new Label("Run:"), runCombo);
        topBar.setPadding(new Insets(8));
        pane.setTop(topBar);

        runCombo.valueProperty().addListener((obs, old, val) -> {
            if (val != null && val >= 0 && val < result.getRunCount()) {
                BorderPane chartPane = buildRunChart(result.getResult(val));
                pane.setCenter(chartPane);
            }
        });

        if (!indices.isEmpty()) {
            runCombo.setValue(0);
        }

        return pane;
    }

    private BorderPane buildRunChart(RunResult run) {
        LineChart<Number, Number> chart = ChartUtils.createLineChart(timeStepLabel, "Value");

        List<String> allNames = new ArrayList<>();
        allNames.addAll(run.getStockNames());
        allNames.addAll(run.getVariableNames());

        List<XYChart.Series<Number, Number>> allSeries = new ArrayList<>();

        for (int c = 0; c < allNames.size(); c++) {
            boolean isStock = c < run.getStockNames().size();
            int colIndex = isStock ? c : c - run.getStockNames().size();
            if (ChartUtils.isSimulationSetting(allNames.get(c))) {
                continue;
            }

            XYChart.Series<Number, Number> series = new XYChart.Series<>();
            series.setName(allNames.get(c));

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


        ScrollPane sidebarScroll = ChartUtils.buildSeriesToggleSidebar(allSeries);

        MenuItem saveItem = ChartUtils.createPngMenuItem(chart, "multi_sweep_chart.png",
                this::getOwnerWindow);
        MenuItem exportTs = ChartUtils.createCsvMenuItem("Export CSV (Time Series)...",
                "multi_sweep_timeseries.csv", this::getOwnerWindow,
                file -> result.writeTimeSeriesCsv(file.getAbsolutePath()));
        ChartUtils.attachContextMenu(chart, saveItem, exportTs);

        BorderPane chartPane = new BorderPane();
        chartPane.setCenter(chart);
        chartPane.setRight(sidebarScroll);
        return chartPane;
    }

    private Window getOwnerWindow() {
        return getScene() != null ? getScene().getWindow() : null;
    }
}

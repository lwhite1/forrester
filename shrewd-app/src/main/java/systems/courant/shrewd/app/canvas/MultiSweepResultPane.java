package systems.courant.shrewd.app.canvas;

import systems.courant.shrewd.sweep.MultiSweepResult;
import systems.courant.shrewd.sweep.RunResult;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
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
import javafx.scene.layout.VBox;
import systems.courant.shrewd.app.LastDirectoryStore;

import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javafx.stage.FileChooser;

import javax.imageio.ImageIO;

import javafx.embed.swing.SwingFXUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Displays multi-parameter sweep results with a Summary table tab and a
 * Time Series chart tab. The summary shows one row per parameter combination
 * with final and max stock values. The time series tab lets the user select
 * a specific run to chart.
 */
public class MultiSweepResultPane extends BorderPane {

    private final MultiSweepResult result;
    private LineChart<Number, Number> currentChart;

    public MultiSweepResultPane(MultiSweepResult result) {
        this.result = result;

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
        MenuItem copySummary = new MenuItem("Copy to Clipboard (Summary)");
        copySummary.setOnAction(e -> ClipboardExporter.copyMultiSweepSummary(result));
        contextMenu.getItems().addAll(exportSummary, copySummary);
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
        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("Step");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Value");

        LineChart<Number, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setCreateSymbols(false);
        chart.setAnimated(false);
        chart.setLegendVisible(false);

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
        contextMenu.getItems().addAll(saveItem, exportTs);
        chart.setOnContextMenuRequested(e ->
                contextMenu.show(chart, e.getScreenX(), e.getScreenY()));

        BorderPane chartPane = new BorderPane();
        chartPane.setCenter(chart);
        chartPane.setRight(sidebarScroll);
        return chartPane;
    }

    private void saveChartAsPng() {
        if (currentChart == null) {
            return;
        }
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Chart as PNG");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PNG Image", "*.png"));
        fileChooser.setInitialFileName("multi_sweep_chart.png");
        LastDirectoryStore.applyExportDirectory(fileChooser);

        File file = fileChooser.showSaveDialog(getScene() != null ? getScene().getWindow() : null);
        if (file != null) {
            LastDirectoryStore.recordExportDirectory(file);
            WritableImage image = currentChart.snapshot(new SnapshotParameters(), null);
            try {
                ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
            } catch (IOException e) {
                new Alert(Alert.AlertType.ERROR,
                        "Failed to save image: " + e.getMessage()).showAndWait();
            }
        }
    }

    private void exportSummaryCsv() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Summary CSV");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        fileChooser.setInitialFileName("multi_sweep_summary.csv");
        LastDirectoryStore.applyExportDirectory(fileChooser);

        File file = fileChooser.showSaveDialog(getScene() != null ? getScene().getWindow() : null);
        if (file != null) {
            LastDirectoryStore.recordExportDirectory(file);
            try {
                result.writeSummaryCsv(file.getAbsolutePath());
            } catch (java.io.UncheckedIOException e) {
                new Alert(Alert.AlertType.ERROR,
                        "Failed to export CSV: " + e.getMessage()).showAndWait();
            }
        }
    }

    private void exportTimeSeriesCsv() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Time Series CSV");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        fileChooser.setInitialFileName("multi_sweep_timeseries.csv");
        LastDirectoryStore.applyExportDirectory(fileChooser);

        File file = fileChooser.showSaveDialog(getScene() != null ? getScene().getWindow() : null);
        if (file != null) {
            LastDirectoryStore.recordExportDirectory(file);
            try {
                result.writeTimeSeriesCsv(file.getAbsolutePath());
            } catch (java.io.UncheckedIOException e) {
                new Alert(Alert.AlertType.ERROR,
                        "Failed to export CSV: " + e.getMessage()).showAndWait();
            }
        }
    }
}

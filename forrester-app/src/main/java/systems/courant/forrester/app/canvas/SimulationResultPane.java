package systems.courant.forrester.app.canvas;

import systems.courant.forrester.app.LastDirectoryStore;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import com.opencsv.CSVWriter;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

/**
 * Embeddable pane displaying simulation results as a chart and table.
 * Supports ghost overlays from previous simulation runs.
 */
public class SimulationResultPane extends BorderPane {

    private static final double GHOST_OPACITY = 0.25;

    private LineChart<Number, Number> chart;
    private SimulationRunner.SimulationResult simulationResult;
    private Consumer<String> onVariableClicked;

    public SimulationResultPane(SimulationRunner.SimulationResult result,
                                List<SimulationRunner.SimulationResult> ghostRuns,
                                Runnable clearHistory) {
        this.simulationResult = result;
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab tableTab = new Tab("Table", buildTable(result));
        Tab chartTab = new Tab("Chart", buildChartPane(result, ghostRuns, clearHistory));
        tabPane.getTabs().addAll(tableTab, chartTab);

        setCenter(tabPane);
    }

    /**
     * Sets a callback invoked when a variable name in the chart legend is clicked.
     * The callback receives the variable name (matching an element name on the canvas).
     */
    public void setOnVariableClicked(Consumer<String> callback) {
        this.onVariableClicked = callback;
    }

    private TableView<double[]> buildTable(SimulationRunner.SimulationResult result) {
        TableView<double[]> table = new TableView<>();
        List<String> columns = result.columnNames();

        for (int c = 0; c < columns.size(); c++) {
            final int colIndex = c;
            TableColumn<double[], String> col = new TableColumn<>(columns.get(c));
            col.setCellValueFactory(data -> {
                double[] row = data.getValue();
                if (colIndex < row.length) {
                    double val = row[colIndex];
                    if (colIndex == 0) {
                        return new SimpleStringProperty(String.valueOf((int) val));
                    }
                    return new SimpleStringProperty(ChartUtils.formatNumber(val));
                }
                return new SimpleStringProperty("");
            });
            col.setPrefWidth(colIndex == 0 ? 60 : 120);
            table.getColumns().add(col);
        }

        table.setItems(FXCollections.observableArrayList(result.rows()));
        return table;
    }

    private BorderPane buildChartPane(SimulationRunner.SimulationResult result,
                                      List<SimulationRunner.SimulationResult> ghostRuns,
                                      Runnable clearHistory) {
        List<String> columns = result.columnNames();
        List<double[]> rows = result.rows();

        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel(columns.isEmpty() ? "Step" : columns.getFirst());

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel(resolveYAxisLabel(columns, result.units()));

        chart = new LineChart<>(xAxis, yAxis);
        chart.setCreateSymbols(false);
        chart.setAnimated(false);
        chart.setLegendVisible(false);

        // --- Ghost series from previous runs ---
        List<XYChart.Series<Number, Number>> ghostSeries = new ArrayList<>();
        for (int g = 0; g < ghostRuns.size(); g++) {
            SimulationRunner.SimulationResult ghost = ghostRuns.get(g);
            List<String> ghostColumns = ghost.columnNames();
            List<double[]> ghostRows = ghost.rows();
            for (int c = 1; c < ghostColumns.size(); c++) {
                XYChart.Series<Number, Number> series = new XYChart.Series<>();
                series.setName(ghostColumns.get(c) + " (run " + (g + 1) + ")");
                for (double[] row : ghostRows) {
                    if (c < row.length) {
                        series.getData().add(new XYChart.Data<>(row[0], row[c]));
                    }
                }
                ghostSeries.add(series);
            }
        }

        // Add ghost series first so they render behind current
        chart.getData().addAll(ghostSeries);

        // Style ghost series with low opacity
        for (XYChart.Series<Number, Number> series : ghostSeries) {
            series.nodeProperty().addListener((obs, oldNode, newNode) -> {
                if (newNode != null) {
                    newNode.setStyle("-fx-stroke: #888888; -fx-opacity: " + GHOST_OPACITY + ";");
                }
            });
            if (series.getNode() != null) {
                series.getNode().setStyle("-fx-stroke: #888888; -fx-opacity: " + GHOST_OPACITY + ";");
            }
        }

        // --- Current run series ---
        List<XYChart.Series<Number, Number>> currentSeries = new ArrayList<>();
        List<String> behaviorModes = new ArrayList<>();
        for (int c = 1; c < columns.size(); c++) {
            XYChart.Series<Number, Number> series = new XYChart.Series<>();
            series.setName(columns.get(c));
            double[] colValues = new double[rows.size()];
            for (int r = 0; r < rows.size(); r++) {
                double[] row = rows.get(r);
                if (c < row.length) {
                    series.getData().add(new XYChart.Data<>(row[0], row[c]));
                    colValues[r] = row[c];
                }
            }
            currentSeries.add(series);
            behaviorModes.add(BehaviorModeClassifier.classify(colValues));
        }

        chart.getData().addAll(currentSeries);
        ChartUtils.applySeriesColors(currentSeries);

        // --- Sidebar ---
        VBox sidebar = new VBox(6);
        sidebar.setPadding(new Insets(10));

        // Select All / None controls
        List<CheckBox> seriesCheckBoxes = new ArrayList<>();

        Hyperlink selectAll = new Hyperlink("All");
        selectAll.setStyle("-fx-font-size: 11px;");
        selectAll.setOnAction(e -> seriesCheckBoxes.forEach(cb -> cb.setSelected(true)));

        Hyperlink selectNone = new Hyperlink("None");
        selectNone.setStyle("-fx-font-size: 11px;");
        selectNone.setOnAction(e -> seriesCheckBoxes.forEach(cb -> cb.setSelected(false)));

        HBox selectionBar = new HBox(4, new Label("Show:"), selectAll, selectNone);
        selectionBar.setAlignment(Pos.CENTER_LEFT);
        sidebar.getChildren().add(selectionBar);

        // Current run visibility toggles with clickable labels
        for (int i = 0; i < currentSeries.size(); i++) {
            XYChart.Series<Number, Number> series = currentSeries.get(i);
            String color = ChartUtils.SERIES_COLORS.get(i % ChartUtils.SERIES_COLORS.size());

            CheckBox cb = new CheckBox();
            cb.setSelected(true);
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
            seriesCheckBoxes.add(cb);

            Label nameLabel = new Label(series.getName());
            nameLabel.setId("seriesLabel-" + series.getName());
            nameLabel.setStyle("-fx-text-fill: " + color + "; -fx-cursor: hand;");
            nameLabel.setOnMouseClicked(e -> {
                if (onVariableClicked != null) {
                    onVariableClicked.accept(series.getName());
                }
            });

            HBox row = new HBox(4, cb, nameLabel);
            row.setAlignment(Pos.CENTER_LEFT);

            String mode = behaviorModes.get(i);
            if (!mode.isEmpty()) {
                Label modeLabel = new Label(mode);
                modeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #888;");
                modeLabel.setPadding(new Insets(0, 0, 0, 22));
                sidebar.getChildren().addAll(row, modeLabel);
            } else {
                sidebar.getChildren().add(row);
            }
        }

        // Ghost controls (only if there are ghost runs)
        if (!ghostRuns.isEmpty()) {
            sidebar.getChildren().add(new Separator());

            Label ghostLabel = new Label(ghostRuns.size() == 1
                    ? "1 previous run"
                    : ghostRuns.size() + " previous runs");
            ghostLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #888;");
            sidebar.getChildren().add(ghostLabel);

            CheckBox showGhosts = new CheckBox("Show previous");
            showGhosts.setSelected(true);
            showGhosts.setStyle("-fx-font-size: 11px; -fx-text-fill: #888;");
            showGhosts.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
                for (XYChart.Series<Number, Number> series : ghostSeries) {
                    if (series.getNode() != null) {
                        series.getNode().setVisible(isSelected);
                    }
                    series.getData().forEach(d -> {
                        if (d.getNode() != null) {
                            d.getNode().setVisible(isSelected);
                        }
                    });
                }
            });
            sidebar.getChildren().add(showGhosts);

            Button clearButton = new Button("Clear History");
            clearButton.setStyle("-fx-font-size: 11px;");
            clearButton.setOnAction(e -> {
                clearHistory.run();
                // Remove ghost series from chart
                chart.getData().removeAll(ghostSeries);
                ghostSeries.clear();
                // Remove ghost controls from sidebar
                // Find and remove everything after the separator
                int sepIndex = -1;
                for (int i = 0; i < sidebar.getChildren().size(); i++) {
                    if (sidebar.getChildren().get(i) instanceof Separator) {
                        sepIndex = i;
                        break;
                    }
                }
                if (sepIndex >= 0) {
                    sidebar.getChildren().remove(sepIndex, sidebar.getChildren().size());
                }
            });
            sidebar.getChildren().add(clearButton);
        }

        ScrollPane sidebarScroll = new ScrollPane(sidebar);
        sidebarScroll.setFitToWidth(true);
        sidebarScroll.setPrefWidth(180);

        ContextMenu contextMenu = new ContextMenu();
        MenuItem saveItem = new MenuItem("Save as PNG...");
        saveItem.setOnAction(e -> saveChartAsPng());
        MenuItem exportCsvItem = new MenuItem("Export CSV...");
        exportCsvItem.setOnAction(e -> exportCsv());
        contextMenu.getItems().addAll(saveItem, exportCsvItem);
        chart.setOnContextMenuRequested(e ->
                contextMenu.show(chart, e.getScreenX(), e.getScreenY()));

        BorderPane pane = new BorderPane();
        pane.setCenter(chart);
        pane.setRight(sidebarScroll);
        return pane;
    }

    private void exportCsv() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export CSV");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        fileChooser.setInitialFileName("simulation.csv");
        LastDirectoryStore.applyExportDirectory(fileChooser);

        File file = fileChooser.showSaveDialog(getScene() != null ? getScene().getWindow() : null);
        if (file != null) {
            LastDirectoryStore.recordExportDirectory(file);
            try (CSVWriter writer = new CSVWriter(new OutputStreamWriter(
                    Files.newOutputStream(file.toPath()), StandardCharsets.UTF_8))) {
                List<String> columns = simulationResult.columnNames();
                writer.writeNext(columns.toArray(new String[0]));
                for (double[] row : simulationResult.rows()) {
                    String[] line = new String[row.length];
                    for (int i = 0; i < row.length; i++) {
                        line[i] = (i == 0) ? String.valueOf((int) row[i])
                                : String.valueOf(row[i]);
                    }
                    writer.writeNext(line);
                }
            } catch (IOException e) {
                new Alert(Alert.AlertType.ERROR,
                        "Failed to export CSV: " + e.getMessage()).showAndWait();
            }
        }
    }

    /**
     * If all plotted variables share the same non-empty unit, returns "Value (unit)".
     * Otherwise returns "Value".
     */
    private static String resolveYAxisLabel(List<String> columns, Map<String, String> units) {
        if (units.isEmpty() || columns.size() <= 1) {
            return "Value";
        }
        Set<String> distinctUnits = columns.stream()
                .skip(1) // skip "Step"
                .map(name -> units.getOrDefault(name, ""))
                .filter(u -> !u.isEmpty())
                .collect(Collectors.toSet());
        if (distinctUnits.size() == 1) {
            return "Value (" + distinctUnits.iterator().next() + ")";
        }
        return "Value";
    }

    private void saveChartAsPng() {
        if (chart == null) {
            return;
        }
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Chart as PNG");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PNG Image", "*.png"));
        fileChooser.setInitialFileName("simulation_chart.png");
        LastDirectoryStore.applyExportDirectory(fileChooser);

        File file = fileChooser.showSaveDialog(getScene() != null ? getScene().getWindow() : null);
        if (file != null) {
            LastDirectoryStore.recordExportDirectory(file);
            WritableImage image = chart.snapshot(new SnapshotParameters(), null);
            try {
                ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
            } catch (IOException e) {
                Alert alert = new Alert(Alert.AlertType.ERROR,
                        "Failed to save image: " + e.getMessage());
                alert.showAndWait();
            }
        }
    }

}

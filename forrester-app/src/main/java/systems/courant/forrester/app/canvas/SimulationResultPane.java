package systems.courant.forrester.app.canvas;

import systems.courant.forrester.app.LastDirectoryStore;
import systems.courant.forrester.model.def.FlowDef;

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
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import com.opencsv.CSVWriter;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

/**
 * Embeddable pane displaying simulation results as a chart and table.
 * Supports ghost overlays from previous simulation runs.
 *
 * <p>Stocks (levels) are rendered with solid lines and variables (auxiliaries)
 * with dashed lines. The sidebar groups series under "Stocks" and "Variables"
 * section headers when both types are present.
 */
public class SimulationResultPane extends BorderPane {

    private static final String DASHED_STROKE = "-fx-stroke-dash-array: 8 4;";

    private LineChart<Number, Number> chart;
    private SimulationRunner.SimulationResult simulationResult;
    private final List<FlowDef> flows;
    private Consumer<String> onVariableClicked;

    public SimulationResultPane(SimulationRunner.SimulationResult result,
                                List<FlowDef> flows,
                                List<GhostRun> ghostRuns,
                                Runnable clearHistory) {
        this.simulationResult = result;
        this.flows = flows != null ? flows : List.of();
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
                                      List<GhostRun> ghostRuns,
                                      Runnable clearHistory) {
        List<String> columns = result.columnNames();
        List<double[]> rows = result.rows();
        Set<String> stockNames = result.stockNames();

        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel(columns.isEmpty() ? "Step" : columns.getFirst());

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel(resolveYAxisLabel(columns, result.units()));

        chart = new LineChart<>(xAxis, yAxis);
        chart.setCreateSymbols(false);
        chart.setAnimated(false);
        chart.setLegendVisible(false);

        // --- Ghost series from previous runs (one group per ghost) ---
        List<List<XYChart.Series<Number, Number>>> ghostSeriesGroups = new ArrayList<>();
        List<XYChart.Series<Number, Number>> allGhostSeries = new ArrayList<>();
        for (int g = 0; g < ghostRuns.size(); g++) {
            GhostRun ghost = ghostRuns.get(g);
            SimulationRunner.SimulationResult ghostResult = ghost.result();
            List<String> ghostColumns = ghostResult.columnNames();
            List<double[]> ghostRows = ghostResult.rows();
            String ghostColor = ChartUtils.GHOST_COLORS.get(
                    ghost.colorIndex() % ChartUtils.GHOST_COLORS.size());
            String tooltipText = ghost.tooltipText();

            List<XYChart.Series<Number, Number>> groupSeries = new ArrayList<>();
            for (int c = 1; c < ghostColumns.size(); c++) {
                XYChart.Series<Number, Number> series = new XYChart.Series<>();
                series.setName(ghostColumns.get(c) + " (" + ghost.name() + ")");
                for (double[] row : ghostRows) {
                    if (c < row.length) {
                        series.getData().add(new XYChart.Data<>(row[0], row[c]));
                    }
                }
                // Style with ghost's assigned color
                String style = "-fx-stroke: " + ghostColor + "; -fx-opacity: "
                        + ChartUtils.GHOST_OPACITY + ";";
                series.nodeProperty().addListener((obs, oldNode, newNode) -> {
                    if (newNode != null) {
                        newNode.setStyle(style);
                        Tooltip.install(newNode, new Tooltip(tooltipText));
                    }
                });
                if (series.getNode() != null) {
                    series.getNode().setStyle(style);
                    Tooltip.install(series.getNode(), new Tooltip(tooltipText));
                }
                groupSeries.add(series);
            }
            ghostSeriesGroups.add(groupSeries);
            allGhostSeries.addAll(groupSeries);
        }

        // Add ghost series first so they render behind current
        chart.getData().addAll(allGhostSeries);

        // --- Current run series ---
        List<XYChart.Series<Number, Number>> currentSeries = new ArrayList<>();
        List<String> behaviorModes = new ArrayList<>();
        List<Boolean> isStock = new ArrayList<>();
        for (int c = 1; c < columns.size(); c++) {
            XYChart.Series<Number, Number> series = new XYChart.Series<>();
            String name = columns.get(c);
            series.setName(name);
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
            isStock.add(stockNames.contains(name));
        }

        chart.getData().addAll(currentSeries);
        ChartUtils.applySeriesColors(currentSeries);

        // Apply dashed stroke to non-stock (variable/auxiliary) series
        if (!stockNames.isEmpty()) {
            for (int i = 0; i < currentSeries.size(); i++) {
                if (!isStock.get(i)) {
                    applyDashedStyle(currentSeries.get(i));
                }
            }
        }

        // --- Net flow series for each stock ---
        Map<String, double[]> netFlows = computeNetFlows(columns, rows, flows, stockNames);
        List<XYChart.Series<Number, Number>> netFlowSeries = new ArrayList<>();
        Map<XYChart.Series<Number, Number>, Integer> netFlowColorIndex = new LinkedHashMap<>();
        for (int i = 0; i < currentSeries.size(); i++) {
            String seriesName = currentSeries.get(i).getName();
            double[] netValues = netFlows.get(seriesName);
            if (netValues != null) {
                XYChart.Series<Number, Number> nfSeries = new XYChart.Series<>();
                nfSeries.setName(seriesName + " (net flow)");
                for (int r = 0; r < rows.size(); r++) {
                    nfSeries.getData().add(new XYChart.Data<>(rows.get(r)[0], netValues[r]));
                }
                netFlowSeries.add(nfSeries);
                netFlowColorIndex.put(nfSeries, i);
            }
        }

        if (!netFlowSeries.isEmpty()) {
            chart.getData().addAll(netFlowSeries);
            for (XYChart.Series<Number, Number> nf : netFlowSeries) {
                int ci = netFlowColorIndex.get(nf);
                String nfColor = ChartUtils.SERIES_COLORS.get(ci % ChartUtils.SERIES_COLORS.size());
                applyNetFlowStyle(nf, nfColor);
                setSeriesVisible(nf, false);
            }
        }

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

        // Group series by stock vs variable when stock names are available
        boolean hasGrouping = !stockNames.isEmpty()
                && currentSeries.stream().anyMatch(s -> !stockNames.contains(s.getName()));

        if (hasGrouping) {
            addSectionHeader(sidebar, "Stocks");
            for (int i = 0; i < currentSeries.size(); i++) {
                if (isStock.get(i)) {
                    addSeriesRow(sidebar, currentSeries.get(i), i,
                            behaviorModes.get(i), seriesCheckBoxes);
                }
            }
            addSectionHeader(sidebar, "Variables");
            for (int i = 0; i < currentSeries.size(); i++) {
                if (!isStock.get(i)) {
                    addSeriesRow(sidebar, currentSeries.get(i), i,
                            behaviorModes.get(i), seriesCheckBoxes);
                }
            }
        } else {
            for (int i = 0; i < currentSeries.size(); i++) {
                addSeriesRow(sidebar, currentSeries.get(i), i,
                        behaviorModes.get(i), seriesCheckBoxes);
            }
        }

        // Net flow toggle
        if (!netFlowSeries.isEmpty()) {
            sidebar.getChildren().add(new Separator());
            CheckBox showNetFlows = new CheckBox("Show net flows");
            showNetFlows.setId("showNetFlows");
            showNetFlows.setSelected(false);
            showNetFlows.setStyle("-fx-font-size: 11px;");
            showNetFlows.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
                for (XYChart.Series<Number, Number> nf : netFlowSeries) {
                    setSeriesVisible(nf, isSelected);
                }
            });
            sidebar.getChildren().add(showNetFlows);
        }

        // Ghost controls (only if there are ghost runs)
        if (!ghostRuns.isEmpty()) {
            sidebar.getChildren().add(new Separator());

            Label ghostHeader = new Label("Previous Runs");
            ghostHeader.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #555;");
            sidebar.getChildren().add(ghostHeader);

            // Per-ghost entries: color swatch, editable name, visibility toggle
            for (int g = 0; g < ghostRuns.size(); g++) {
                GhostRun ghost = ghostRuns.get(g);
                List<XYChart.Series<Number, Number>> groupSeries = ghostSeriesGroups.get(g);
                String ghostColor = ChartUtils.GHOST_COLORS.get(
                        ghost.colorIndex() % ChartUtils.GHOST_COLORS.size());

                // Color swatch
                Region swatch = new Region();
                swatch.setMinSize(12, 12);
                swatch.setMaxSize(12, 12);
                swatch.setStyle("-fx-background-color: " + ghostColor
                        + "; -fx-background-radius: 2;");

                // Editable name label (click to edit)
                Label nameLabel = new Label(ghost.name());
                nameLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + ghostColor
                        + "; -fx-cursor: hand;");
                nameLabel.setMaxWidth(120);
                nameLabel.setTooltip(new Tooltip(ghost.tooltipText()));

                // Click to edit: replace label with text field
                nameLabel.setOnMouseClicked(e -> {
                    if (e.getClickCount() == 2) {
                        TextField editor = new TextField(nameLabel.getText());
                        editor.setStyle("-fx-font-size: 11px;");
                        editor.setPrefWidth(120);
                        HBox parentRow = (HBox) nameLabel.getParent();
                        int labelIndex = parentRow.getChildren().indexOf(nameLabel);
                        parentRow.getChildren().set(labelIndex, editor);
                        editor.requestFocus();
                        editor.selectAll();

                        Runnable commit = () -> {
                            String newName = editor.getText().trim();
                            if (!newName.isEmpty()) {
                                nameLabel.setText(newName);
                            }
                            parentRow.getChildren().set(labelIndex, nameLabel);
                        };
                        editor.setOnAction(ev -> commit.run());
                        editor.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
                            if (!isFocused) {
                                commit.run();
                            }
                        });
                    }
                });

                // Visibility toggle
                CheckBox ghostCb = new CheckBox();
                ghostCb.setSelected(true);
                ghostCb.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
                    for (XYChart.Series<Number, Number> series : groupSeries) {
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

                HBox ghostRow = new HBox(4, ghostCb, swatch, nameLabel);
                ghostRow.setAlignment(Pos.CENTER_LEFT);
                sidebar.getChildren().add(ghostRow);
            }

            Button clearButton = new Button("Clear History");
            clearButton.setStyle("-fx-font-size: 11px;");
            clearButton.setOnAction(e -> {
                clearHistory.run();
                chart.getData().removeAll(allGhostSeries);
                allGhostSeries.clear();
                ghostSeriesGroups.clear();
                // Remove everything after the separator
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
        sidebarScroll.setPrefWidth(200);

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

    private void addSectionHeader(VBox sidebar, String title) {
        Label header = new Label(title);
        header.setStyle(Styles.FIELD_LABEL + " -fx-text-fill: #555; -fx-padding: 4 0 2 0;");
        sidebar.getChildren().add(header);
    }

    private void addSeriesRow(VBox sidebar, XYChart.Series<Number, Number> series,
                              int colorIndex, String behaviorMode,
                              List<CheckBox> seriesCheckBoxes) {
        String color = ChartUtils.SERIES_COLORS.get(colorIndex % ChartUtils.SERIES_COLORS.size());

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

        if (!behaviorMode.isEmpty()) {
            Label modeLabel = new Label(behaviorMode);
            modeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #888;");
            modeLabel.setPadding(new Insets(0, 0, 0, 22));
            sidebar.getChildren().addAll(row, modeLabel);
        } else {
            sidebar.getChildren().add(row);
        }
    }

    private static void applyDashedStyle(XYChart.Series<Number, Number> series) {
        series.nodeProperty().addListener((obs, oldNode, newNode) -> {
            if (newNode != null) {
                newNode.setStyle(DASHED_STROKE);
            }
        });
        if (series.getNode() != null) {
            series.getNode().setStyle(DASHED_STROKE);
        }
    }

    /**
     * Computes net flow values for each stock from simulation data and flow definitions.
     * Net flow into stock S = sum(flows where sink==S) - sum(flows where source==S).
     *
     * @return map from stock name to net flow time series (one value per row)
     */
    static Map<String, double[]> computeNetFlows(List<String> columns, List<double[]> rows,
                                                  List<FlowDef> flows, Set<String> stockNames) {
        Map<String, double[]> result = new LinkedHashMap<>();
        if (flows == null || flows.isEmpty() || stockNames.isEmpty()) {
            return result;
        }

        Map<String, Integer> colIndex = new LinkedHashMap<>();
        for (int i = 0; i < columns.size(); i++) {
            colIndex.put(columns.get(i), i);
        }

        for (String stockName : stockNames) {
            List<Integer> inflowIndices = new ArrayList<>();
            List<Integer> outflowIndices = new ArrayList<>();
            for (FlowDef flow : flows) {
                Integer idx = colIndex.get(flow.name());
                if (idx == null) {
                    continue;
                }
                if (stockName.equals(flow.sink())) {
                    inflowIndices.add(idx);
                }
                if (stockName.equals(flow.source())) {
                    outflowIndices.add(idx);
                }
            }

            if (inflowIndices.isEmpty() && outflowIndices.isEmpty()) {
                continue;
            }

            double[] netValues = new double[rows.size()];
            for (int r = 0; r < rows.size(); r++) {
                double[] row = rows.get(r);
                double net = 0;
                for (int idx : inflowIndices) {
                    if (idx < row.length) {
                        net += row[idx];
                    }
                }
                for (int idx : outflowIndices) {
                    if (idx < row.length) {
                        net -= row[idx];
                    }
                }
                netValues[r] = net;
            }
            result.put(stockName, netValues);
        }
        return result;
    }

    private static void applyNetFlowStyle(XYChart.Series<Number, Number> series, String color) {
        String style = "-fx-stroke: " + color + "; " + DASHED_STROKE + " -fx-opacity: 0.7;";
        series.nodeProperty().addListener((obs, oldNode, newNode) -> {
            if (newNode != null) {
                newNode.setStyle(style);
            }
        });
        if (series.getNode() != null) {
            series.getNode().setStyle(style);
        }
    }

    private static void setSeriesVisible(XYChart.Series<Number, Number> series, boolean visible) {
        if (series.getNode() != null) {
            series.getNode().setVisible(visible);
        }
        series.getData().forEach(d -> {
            if (d.getNode() != null) {
                d.getNode().setVisible(visible);
            }
        });
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

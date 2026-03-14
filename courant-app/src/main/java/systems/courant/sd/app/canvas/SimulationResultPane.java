package systems.courant.sd.app.canvas;

import systems.courant.sd.app.LastDirectoryStore;
import systems.courant.sd.io.ReferenceDataCsvReader;
import systems.courant.sd.model.def.FlowDef;
import systems.courant.sd.model.def.ReferenceDataset;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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
import javafx.beans.property.DoubleProperty;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
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
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Embeddable pane displaying simulation results as a chart and table.
 * Supports ghost overlays from previous simulation runs.
 *
 * <p>Stocks (levels) are rendered with solid lines and variables (variables)
 * with dashed lines. The sidebar groups series under "Stocks" and "Variables"
 * section headers when both types are present.
 */
public class SimulationResultPane extends BorderPane {

    private static final Logger log = LoggerFactory.getLogger(SimulationResultPane.class);
    private static final String DASHED_STROKE = "-fx-stroke-dash-array: 8 4;";
    private static final String REFERENCE_STROKE = "-fx-stroke-dash-array: 3 3;";
    /** Muted/darker versions of series colors for reference data overlay. */
    private static final List<String> REFERENCE_COLORS = List.of(
            "#0d3b66", "#b35900", "#1a661a", "#8b1a1a", "#5c3d7a",
            "#5a3520", "#993d71", "#4d4d4d", "#7a7a0f", "#0d7a82"
    );

    private LineChart<Number, Number> chart;
    private ChartTimeCursor timeCursor;
    private SimulationRunner.SimulationResult simulationResult;
    private final List<FlowDef> flows;
    private final List<ReferenceDataset> referenceDatasets;
    private Consumer<String> onVariableClicked;
    private Consumer<ReferenceDataset> onReferenceDataImported;

    public SimulationResultPane(SimulationRunner.SimulationResult result,
                                List<FlowDef> flows,
                                List<GhostRun> ghostRuns,
                                Runnable clearHistory) {
        this(result, flows, ghostRuns, clearHistory, List.of());
    }

    public SimulationResultPane(SimulationRunner.SimulationResult result,
                                List<FlowDef> flows,
                                List<GhostRun> ghostRuns,
                                Runnable clearHistory,
                                List<ReferenceDataset> referenceDatasets) {
        this.simulationResult = result;
        this.flows = flows != null ? flows : List.of();
        this.referenceDatasets = referenceDatasets != null ? referenceDatasets : List.of();
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

    /**
     * Sets a callback invoked when reference data is imported via the chart context menu.
     * The callback receives the imported dataset so it can be persisted with the model.
     */
    public void setOnReferenceDataImported(Consumer<ReferenceDataset> callback) {
        this.onReferenceDataImported = callback;
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
                        return new SimpleStringProperty(formatTimeStep(val));
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
        xAxis.setAutoRanging(false);
        if (!rows.isEmpty()) {
            double xMin = rows.getFirst()[0];
            double xMax = rows.getLast()[0];
            xAxis.setLowerBound(xMin);
            xAxis.setUpperBound(xMax);
            xAxis.setTickUnit(niceTickUnit(xMax - xMin));
        }

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel(resolveYAxisLabel(columns, result.units()));
        yAxis.setAutoRanging(false);

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

        // Y-axis rescaling: recompute bounds from visible series (#542)
        Runnable rescaleYAxis = () -> {
            double yMin = Double.MAX_VALUE;
            double yMax = -Double.MAX_VALUE;
            for (XYChart.Series<Number, Number> s : chart.getData()) {
                if (s.getNode() != null && !s.getNode().isVisible()) {
                    continue;
                }
                for (XYChart.Data<Number, Number> d : s.getData()) {
                    double val = d.getYValue().doubleValue();
                    if (val < yMin) {
                        yMin = val;
                    }
                    if (val > yMax) {
                        yMax = val;
                    }
                }
            }
            if (yMin >= yMax) {
                yMin = yMin - 1;
                yMax = yMax + 1;
            }
            double pad = (yMax - yMin) * 0.05;
            if (pad == 0) {
                pad = 0.5;
            }
            yAxis.setLowerBound(yMin - pad);
            yAxis.setUpperBound(yMax + pad);
            yAxis.setTickUnit(niceTickUnit(yMax - yMin + 2 * pad));
        };
        rescaleYAxis.run();

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
                            behaviorModes.get(i), seriesCheckBoxes, rescaleYAxis);
                }
            }
            addSectionHeader(sidebar, "Variables");
            for (int i = 0; i < currentSeries.size(); i++) {
                if (!isStock.get(i)) {
                    addSeriesRow(sidebar, currentSeries.get(i), i,
                            behaviorModes.get(i), seriesCheckBoxes, rescaleYAxis);
                }
            }
        } else {
            for (int i = 0; i < currentSeries.size(); i++) {
                addSeriesRow(sidebar, currentSeries.get(i), i,
                        behaviorModes.get(i), seriesCheckBoxes, rescaleYAxis);
            }
        }

        // --- Reference data overlay ---
        List<XYChart.Series<Number, Number>> allRefSeries = new ArrayList<>();
        if (!referenceDatasets.isEmpty()) {
            int refColorIdx = 0;
            for (ReferenceDataset refData : referenceDatasets) {
                for (String varName : refData.variableNames()) {
                    XYChart.Series<Number, Number> refSeries = new XYChart.Series<>();
                    refSeries.setName(varName + " (observed)");
                    double[] timeVals = refData.timeValues();
                    double[] dataVals = refData.columns().get(varName);
                    for (int i = 0; i < timeVals.length; i++) {
                        if (!Double.isNaN(dataVals[i])) {
                            refSeries.getData().add(new XYChart.Data<>(timeVals[i], dataVals[i]));
                        }
                    }
                    allRefSeries.add(refSeries);
                    refColorIdx++;
                }
            }
            chart.getData().addAll(allRefSeries);

            // Style reference series: dotted lines in muted colors
            for (int i = 0; i < allRefSeries.size(); i++) {
                XYChart.Series<Number, Number> refSeries = allRefSeries.get(i);
                String refColor = REFERENCE_COLORS.get(i % REFERENCE_COLORS.size());
                String refStyle = "-fx-stroke: " + refColor + "; " + REFERENCE_STROKE;
                refSeries.nodeProperty().addListener((obs, oldNode, newNode) -> {
                    if (newNode != null) {
                        newNode.setStyle(refStyle);
                    }
                });
                if (refSeries.getNode() != null) {
                    refSeries.getNode().setStyle(refStyle);
                }
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
            ghostHeader.setId("ghostRunsHeader");
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
                            int editorIndex = parentRow.getChildren().indexOf(editor);
                            if (editorIndex >= 0) {
                                parentRow.getChildren().set(editorIndex, nameLabel);
                            }
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
                // Remove the ghost section: find the "Previous Runs" header
                // and remove from the separator before it to the end of the
                // ghost section (the Clear History button). Other sections
                // (net flow toggle, reference data) must be preserved.
                int ghostHeaderIndex = -1;
                for (int i = 0; i < sidebar.getChildren().size(); i++) {
                    if (sidebar.getChildren().get(i) instanceof Label label
                            && "ghostRunsHeader".equals(label.getId())) {
                        ghostHeaderIndex = i;
                        break;
                    }
                }
                if (ghostHeaderIndex >= 0) {
                    int startRemove = ghostHeaderIndex;
                    // Include the separator before the header if present
                    if (ghostHeaderIndex > 0
                            && sidebar.getChildren().get(ghostHeaderIndex - 1) instanceof Separator) {
                        startRemove = ghostHeaderIndex - 1;
                    }
                    // Find the Clear History button to determine the end of the ghost section
                    int endRemove = sidebar.getChildren().size();
                    for (int i = ghostHeaderIndex + 1; i < sidebar.getChildren().size(); i++) {
                        if (sidebar.getChildren().get(i) instanceof Button) {
                            endRemove = i + 1;
                            break;
                        }
                    }
                    sidebar.getChildren().remove(startRemove, endRemove);
                }
            });
            sidebar.getChildren().add(clearButton);
        }

        // Reference data sidebar section
        if (!allRefSeries.isEmpty()) {
            sidebar.getChildren().add(new Separator());
            Label refHeader = new Label("Reference Data");
            refHeader.setId("referenceDataHeader");
            refHeader.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #555;");
            sidebar.getChildren().add(refHeader);

            for (int i = 0; i < allRefSeries.size(); i++) {
                XYChart.Series<Number, Number> refSeries = allRefSeries.get(i);
                String refColor = REFERENCE_COLORS.get(i % REFERENCE_COLORS.size());

                CheckBox refCb = new CheckBox();
                refCb.setSelected(true);
                refCb.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
                    setSeriesVisible(refSeries, isSelected);
                });

                Label refLabel = new Label(refSeries.getName());
                refLabel.setStyle("-fx-text-fill: " + refColor + "; -fx-font-size: 11px;");

                HBox refRow = new HBox(4, refCb, refLabel);
                refRow.setAlignment(Pos.CENTER_LEFT);
                sidebar.getChildren().add(refRow);
            }
        }

        ScrollPane sidebarScroll = new ScrollPane(sidebar);
        sidebarScroll.setFitToWidth(true);
        sidebarScroll.setPrefWidth(200);

        ContextMenu contextMenu = new ContextMenu();
        MenuItem saveItem = new MenuItem("Save as PNG...");
        saveItem.setOnAction(e -> saveChartAsPng());
        MenuItem exportCsvItem = new MenuItem("Export CSV...");
        exportCsvItem.setOnAction(e -> exportCsv());
        MenuItem copyItem = new MenuItem("Copy to Clipboard");
        copyItem.setOnAction(e -> ClipboardExporter.copySimulationResult(simulationResult));
        MenuItem importRefItem = new MenuItem("Import Reference Data...");
        importRefItem.setId("importReferenceDataMenuItem");
        importRefItem.setOnAction(e -> importReferenceData());
        contextMenu.getItems().addAll(saveItem, exportCsvItem, copyItem, importRefItem);
        chart.setOnContextMenuRequested(e ->
                contextMenu.show(chart, e.getScreenX(), e.getScreenY()));

        ChartTimeCursor[] cursorHolder = new ChartTimeCursor[1];
        StackPane chartWithCursor = ChartTimeCursor.install(chart, cursorHolder);
        timeCursor = cursorHolder[0];

        BorderPane pane = new BorderPane();
        pane.setCenter(chartWithCursor);
        pane.setRight(sidebarScroll);
        return pane;
    }

    /**
     * Returns the time cursor property for this chart, allowing synchronization
     * with other charts. Value is {@code Double.NaN} when no cursor is active.
     */
    DoubleProperty cursorTimeStepProperty() {
        return timeCursor != null ? timeCursor.cursorTimeStepProperty() : null;
    }

    private void addSectionHeader(VBox sidebar, String title) {
        Label header = new Label(title);
        header.setStyle(Styles.FIELD_LABEL + " -fx-text-fill: #555; -fx-padding: 4 0 2 0;");
        sidebar.getChildren().add(header);
    }

    private void addSeriesRow(VBox sidebar, XYChart.Series<Number, Number> series,
                              int colorIndex, String behaviorMode,
                              List<CheckBox> seriesCheckBoxes, Runnable rescaleYAxis) {
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
            rescaleYAxis.run();
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

    private void importReferenceData() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import Reference Data");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        LastDirectoryStore.applyOpenDirectory(fileChooser);

        File file = fileChooser.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
        if (file == null) {
            return;
        }
        LastDirectoryStore.recordOpenDirectory(file);
        try {
            String name = file.getName().replaceFirst("\\.[^.]+$", "");
            ReferenceDataset dataset = ReferenceDataCsvReader.read(file.toPath(), name);
            if (onReferenceDataImported != null) {
                onReferenceDataImported.accept(dataset);
            }
        } catch (IOException e) {
            log.warn("Failed to import reference data from {}: {}", file, e.getMessage());
            new Alert(Alert.AlertType.ERROR,
                    "Failed to import reference data: " + e.getMessage()).showAndWait();
        }
    }

    private void exportCsv() {
        ChartUtils.showCsvSaveDialog("Export CSV", "simulation.csv",
                getScene() != null ? getScene().getWindow() : null, file -> {
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
                    }
                });
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
        ChartUtils.saveNodeAsPng(chart, "simulation_chart.png",
                getScene() != null ? getScene().getWindow() : null);
    }

    /**
     * Formats a time-step value for the table's step column.
     * Whole numbers are displayed without a decimal point (e.g. "0", "1").
     * Fractional values are displayed with up to 4 decimal places, with
     * trailing zeros stripped (e.g. "0.25", "0.5").
     */
    static String formatTimeStep(double value) {
        if (value == Math.floor(value) && Double.isFinite(value)
                && Math.abs(value) <= Long.MAX_VALUE) {
            return String.valueOf((long) value);
        }
        // Format to 4 decimal places, then strip trailing zeros
        String formatted = String.format(Locale.US, "%.4f", value);
        formatted = formatted.contains(".")
                ? formatted.replaceAll("0+$", "").replaceAll("\\.$", "")
                : formatted;
        return formatted;
    }

    /**
     * Computes a "nice" tick unit for an axis covering the given range,
     * targeting roughly 5–10 tick marks.
     */
    static double niceTickUnit(double range) {
        if (range <= 0) {
            return 1;
        }
        double rawUnit = range / 10;
        double exp = Math.pow(10, Math.floor(Math.log10(rawUnit)));
        double frac = rawUnit / exp;
        if (frac < 1.5) {
            return exp;
        }
        if (frac < 3.5) {
            return 2 * exp;
        }
        if (frac < 7.5) {
            return 5 * exp;
        }
        return 10 * exp;
    }
}

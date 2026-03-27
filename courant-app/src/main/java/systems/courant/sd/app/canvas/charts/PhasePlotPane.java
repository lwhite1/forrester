package systems.courant.sd.app.canvas.charts;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Window;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import systems.courant.sd.app.canvas.ChartUtils;
import systems.courant.sd.app.canvas.GhostRun;
import systems.courant.sd.app.canvas.SimulationRunner;
import systems.courant.sd.app.canvas.Styles;

/**
 * Phase plot pane that plots one simulation variable against another,
 * revealing system behavior modes such as limit cycles, spirals, and
 * convergence that are invisible in time-series charts.
 *
 * <p>Users select X-axis and Y-axis variables via two ComboBoxes.
 * The trajectory is plotted as a connected line with a green circle
 * marking the start point and a red square marking the end point
 * to indicate time direction. Ghost overlays from previous runs
 * are supported for comparison.
 */
public class PhasePlotPane extends BorderPane {

    private static final double START_MARKER_RADIUS = 5.0;
    private static final double END_MARKER_SIZE = 8.0;
    private static final String START_COLOR = "#2ca02c";
    private static final String END_COLOR = "#d62728";
    private static final String TRAJECTORY_COLOR = "#1f77b4";

    private final SimulationRunner.SimulationResult result;
    private final List<GhostRun> ghostRuns;
    private final ComboBox<String> xCombo;
    private final ComboBox<String> yCombo;
    private LineChart<Number, Number> chart;

    public PhasePlotPane(SimulationRunner.SimulationResult result,
                         List<GhostRun> ghostRuns) {
        this.result = result;
        this.ghostRuns = ghostRuns != null ? ghostRuns : List.of();

        List<String> variableNames = getVariableNames(result);

        xCombo = new ComboBox<>(FXCollections.observableArrayList(variableNames));
        xCombo.setId("phasePlotXCombo");
        xCombo.setPromptText("X-axis variable");

        yCombo = new ComboBox<>(FXCollections.observableArrayList(variableNames));
        yCombo.setId("phasePlotYCombo");
        yCombo.setPromptText("Y-axis variable");

        // Auto-select first two variables if available
        if (variableNames.size() >= 2) {
            xCombo.getSelectionModel().select(0);
            yCombo.getSelectionModel().select(1);
        } else if (variableNames.size() == 1) {
            xCombo.getSelectionModel().select(0);
        }

        xCombo.setOnAction(e -> updateChart());
        yCombo.setOnAction(e -> updateChart());

        Label xLabel = new Label("X-axis:");
        xLabel.setStyle(Styles.FIELD_LABEL);
        Label yLabel = new Label("Y-axis:");
        yLabel.setStyle(Styles.FIELD_LABEL);

        HBox controlBar = new HBox(8, xLabel, xCombo, yLabel, yCombo);
        controlBar.setAlignment(Pos.CENTER_LEFT);
        controlBar.setPadding(new Insets(8));
        controlBar.setId("phasePlotControlBar");

        setTop(controlBar);
        updateChart();
    }

    /**
     * Returns the variable names available for axis selection (all columns except "Step").
     */
    public static List<String> getVariableNames(SimulationRunner.SimulationResult result) {
        List<String> names = new ArrayList<>();
        for (int i = 1; i < result.columnNames().size(); i++) {
            String name = result.columnNames().get(i);
            if (!ChartUtils.isSimulationSetting(name)) {
                names.add(name);
            }
        }
        return names;
    }

    /**
     * Extracts the column values for a given variable name from a simulation result.
     * Returns null if the variable is not found.
     */
    public static double[] extractColumn(SimulationRunner.SimulationResult result, String variableName) {
        int colIndex = result.columnNames().indexOf(variableName);
        if (colIndex < 0) {
            return null;
        }
        List<double[]> rows = result.rows();
        double[] values = new double[rows.size()];
        for (int r = 0; r < rows.size(); r++) {
            double[] row = rows.get(r);
            values[r] = colIndex < row.length ? row[colIndex] : 0.0;
        }
        return values;
    }

    private void updateChart() {
        String xVar = xCombo.getValue();
        String yVar = yCombo.getValue();

        if (xVar == null || yVar == null) {
            if (chart != null) {
                setCenter(null);
                chart = null;
            }
            return;
        }

        Map<String, String> units = result.units();
        String xUnit = units.getOrDefault(xVar, "");
        String yUnit = units.getOrDefault(yVar, "");

        String xLabel = xUnit.isEmpty() ? xVar : xVar + " (" + xUnit + ")";
        String yLabel = yUnit.isEmpty() ? yVar : yVar + " (" + yUnit + ")";

        chart = ChartUtils.createLineChart(xLabel, yLabel);
        chart.setId("phasePlotChart");
        chart.setCreateSymbols(true);

        // Phase plots need axes that don't force zero in range
        ((NumberAxis) chart.getXAxis()).setForceZeroInRange(false);
        ((NumberAxis) chart.getYAxis()).setForceZeroInRange(false);

        // Ghost trajectories (rendered first, behind current)
        for (GhostRun ghost : ghostRuns) {
            addGhostTrajectory(ghost, xVar, yVar);
        }

        // Current trajectory
        addCurrentTrajectory(xVar, yVar);

        // Context menu for export
        MenuItem saveItem = ChartUtils.createPngMenuItem(chart, "phase_plot.png",
                this::getOwnerWindow);
        ChartUtils.attachContextMenu(chart, saveItem);

        setCenter(chart);
    }

    private void addCurrentTrajectory(String xVar, String yVar) {
        double[] xValues = extractColumn(result, xVar);
        double[] yValues = extractColumn(result, yVar);
        if (xValues == null || yValues == null || xValues.length == 0) {
            return;
        }

        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName("Current");

        int len = Math.min(xValues.length, yValues.length);
        for (int i = 0; i < len; i++) {
            XYChart.Data<Number, Number> dataPoint = new XYChart.Data<>(xValues[i], yValues[i]);

            if (i == 0) {
                // Start marker: green circle
                Circle startMarker = new Circle(START_MARKER_RADIUS);
                startMarker.setStyle("-fx-fill: " + START_COLOR + ";");
                startMarker.setId("phasePlotStartMarker");
                Tooltip.install(startMarker, new Tooltip("Start (t=0)"));
                dataPoint.setNode(startMarker);
            } else if (i == len - 1) {
                // End marker: red square
                Rectangle endMarker = new Rectangle(END_MARKER_SIZE, END_MARKER_SIZE);
                endMarker.setStyle("-fx-fill: " + END_COLOR + ";");
                endMarker.setId("phasePlotEndMarker");
                Tooltip.install(endMarker, new Tooltip("End (t=" + (len - 1) + ")"));
                dataPoint.setNode(endMarker);
            } else {
                // Invisible placeholder for intermediate points
                Region invisible = new Region();
                invisible.setMaxSize(0, 0);
                invisible.setVisible(false);
                dataPoint.setNode(invisible);
            }

            series.getData().add(dataPoint);
        }

        chart.getData().add(series);

        // Style the trajectory line
        String style = "-fx-stroke: " + TRAJECTORY_COLOR + "; -fx-stroke-width: 2;";
        series.nodeProperty().addListener((obs, oldNode, newNode) -> {
            if (newNode != null) {
                newNode.setStyle(style);
            }
        });
        if (series.getNode() != null) {
            series.getNode().setStyle(style);
        }
    }

    private void addGhostTrajectory(GhostRun ghost, String xVar, String yVar) {
        double[] xValues = extractColumn(ghost.result(), xVar);
        double[] yValues = extractColumn(ghost.result(), yVar);
        if (xValues == null || yValues == null || xValues.length == 0) {
            return;
        }

        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName(ghost.name());

        int len = Math.min(xValues.length, yValues.length);
        for (int i = 0; i < len; i++) {
            series.getData().add(new XYChart.Data<>(xValues[i], yValues[i]));
        }

        chart.getData().add(series);

        // Hide all default symbols for ghost series
        for (XYChart.Data<Number, Number> dp : series.getData()) {
            if (dp.getNode() != null) {
                dp.getNode().setVisible(false);
            }
        }

        String ghostColor = ChartUtils.GHOST_COLORS.get(
                ghost.colorIndex() % ChartUtils.GHOST_COLORS.size());
        String style = "-fx-stroke: " + ghostColor + "; -fx-opacity: "
                + ChartUtils.GHOST_OPACITY + ";";
        series.nodeProperty().addListener((obs, oldNode, newNode) -> {
            if (newNode != null) {
                newNode.setStyle(style);
                Tooltip.install(newNode, new Tooltip(ghost.tooltipText()));
            }
        });
        if (series.getNode() != null) {
            series.getNode().setStyle(style);
            Tooltip.install(series.getNode(), new Tooltip(ghost.tooltipText()));
        }
    }

    private Window getOwnerWindow() {
        return getScene() != null ? getScene().getWindow() : null;
    }
}

package systems.courant.sd.app.canvas;

import systems.courant.sd.model.def.FlowDef;
import systems.courant.sd.model.def.ReferenceDataset;
import systems.courant.sd.model.graph.LoopDominanceAnalysis;
import systems.courant.sd.sweep.MonteCarloResult;
import systems.courant.sd.sweep.MultiSweepResult;
import systems.courant.sd.sweep.OptimizationResult;
import systems.courant.sd.sweep.SweepResult;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.beans.property.DoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import systems.courant.sd.app.canvas.charts.LoopDominancePane;
import systems.courant.sd.app.canvas.charts.MonteCarloResultPane;
import systems.courant.sd.app.canvas.charts.MultiSweepResultPane;
import systems.courant.sd.app.canvas.charts.CalibrationResultPane;
import systems.courant.sd.app.canvas.charts.OptimizationResultPane;
import systems.courant.sd.app.canvas.charts.PhasePlotPane;
import systems.courant.sd.app.canvas.charts.SensitivityPane;
import systems.courant.sd.app.canvas.charts.SimulationResultPane;
import systems.courant.sd.app.canvas.charts.SweepResultPane;
import systems.courant.sd.app.canvas.dialogs.CalibrateDialog;

/**
 * Dashboard panel that displays results from simulation, parameter sweep,
 * Monte Carlo, and optimization runs. Results are shown in separate tabs,
 * created on demand.
 */
public class DashboardPanel extends VBox {

    static final String STALE_BANNER_STYLE =
            "-fx-background-color: #FFF3E0; -fx-border-color: #F59E0B;"
                    + " -fx-border-width: 0 0 1 0; -fx-padding: 6 12 6 12;";
    static final String STALE_BORDER_STYLE =
            "-fx-border-color: #F59E0B; -fx-border-width: 2;";
    static final String STALE_TEXT_STYLE =
            "-fx-text-fill: #92400E; -fx-font-size: 11px;";
    static final String RERUN_LINK_STYLE =
            "-fx-text-fill: #B45309; -fx-font-size: 11px; -fx-underline: true;";

    private final TabPane resultTabs;
    private final StackPane placeholder;
    private final HBox staleBanner;
    private Tab simulationTab;
    private Tab sweepTab;
    private Tab monteCarloTab;
    private Tab optimizationTab;
    private Tab calibrationTab;
    private Tab multiSweepTab;
    private Tab sensitivityTab;
    private Tab dominanceTab;
    private Tab phasePlotTab;
    static final double STALE_DOT_RADIUS = 4.0;
    static final Color STALE_DOT_COLOR = Color.web("#F59E0B");

    private boolean stale;
    private Tab dashboardTab;
    private Runnable rerunAction;
    private DoubleProperty simulationCursor;
    private DoubleProperty dominanceCursor;
    private ChangeListener<Number> simToDomListener;
    private ChangeListener<Number> domToSimListener;
    private Consumer<String> onVariableClicked;
    private Consumer<ReferenceDataset> onReferenceDataImported;
    private boolean updatingCursor;

    /** Previous simulation runs for ghost overlay comparison. Most recent last. */
    private final List<GhostRun> runHistory = new ArrayList<>();
    private static final int MAX_HISTORY = 5;
    private int runCounter;

    public DashboardPanel() {
        Label placeholderLabel = new Label("Run a simulation to see results.");
        placeholderLabel.setStyle(Styles.PLACEHOLDER_TEXT);
        placeholder = new StackPane(placeholderLabel);
        placeholder.setId("dashboardPlaceholder");

        staleBanner = createStaleBanner();
        staleBanner.setId("staleBanner");
        staleBanner.setVisible(false);
        staleBanner.setManaged(false);

        resultTabs = new TabPane();
        resultTabs.setId("dashboardResultTabs");
        resultTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.SELECTED_TAB);
        resultTabs.setVisible(false);
        resultTabs.setManaged(false);

        VBox.setVgrow(placeholder, javafx.scene.layout.Priority.ALWAYS);
        VBox.setVgrow(resultTabs, javafx.scene.layout.Priority.ALWAYS);

        getChildren().addAll(placeholder, staleBanner, resultTabs);
    }

    private HBox createStaleBanner() {
        Label message = new Label("Model changed since last run. Results may not reflect current structure.");
        message.setStyle(STALE_TEXT_STYLE);

        Label rerunLink = new Label("Re-run");
        rerunLink.setId("staleRerunLink");
        rerunLink.setStyle(RERUN_LINK_STYLE);
        rerunLink.setCursor(Cursor.HAND);
        rerunLink.setOnMouseClicked(e -> {
            if (rerunAction != null) {
                rerunAction.run();
            }
        });

        HBox banner = new HBox(8, message, rerunLink);
        banner.setAlignment(Pos.CENTER_LEFT);
        banner.setPadding(new Insets(6, 12, 6, 12));
        banner.setStyle(STALE_BANNER_STYLE);
        return banner;
    }

    /**
     * Sets the parent dashboard tab so its label can be updated when results
     * become stale or are refreshed.
     */
    public void setDashboardTab(Tab tab) {
        this.dashboardTab = tab;
    }

    /**
     * Sets the action to invoke when the user clicks the "Re-run" link
     * on the stale results banner.
     */
    public void setRerunAction(Runnable action) {
        this.rerunAction = action;
    }

    /**
     * Sets a callback invoked when a variable name is clicked in a chart legend.
     * The callback receives the variable name, allowing navigation to the
     * corresponding element on the canvas.
     */
    public void setOnVariableClicked(Consumer<String> callback) {
        this.onVariableClicked = callback;
    }

    /**
     * Sets a callback invoked when reference data is imported via the chart.
     * The callback receives the imported dataset so it can be persisted with the model.
     */
    public void setOnReferenceDataImported(Consumer<ReferenceDataset> callback) {
        this.onReferenceDataImported = callback;
    }

    public void showSimulationResult(SimulationRunner.SimulationResult result) {
        showSimulationResult(result, Map.of(), List.of(), List.of());
    }

    public void showSimulationResult(SimulationRunner.SimulationResult result,
                                     Map<String, Double> parameters) {
        showSimulationResult(result, parameters, List.of(), List.of());
    }

    public void showSimulationResult(SimulationRunner.SimulationResult result,
                                     Map<String, Double> parameters,
                                     List<FlowDef> flows) {
        showSimulationResult(result, parameters, flows, List.of());
    }

    public void showSimulationResult(SimulationRunner.SimulationResult result,
                                     Map<String, Double> parameters,
                                     List<FlowDef> flows,
                                     List<ReferenceDataset> referenceDatasets) {
        clearStale();
        List<GhostRun> ghosts = List.copyOf(runHistory);
        SimulationResultPane pane = new SimulationResultPane(result, flows, ghosts,
                this::clearRunHistory, referenceDatasets);
        pane.setOnVariableClicked(onVariableClicked);
        pane.setOnReferenceDataImported(onReferenceDataImported);
        unbindCursors();
        simulationCursor = pane.cursorTimeStepProperty();
        bindCursors();
        simulationTab = ensureTab(simulationTab, "Simulation", pane);
        resultTabs.getSelectionModel().select(simulationTab);

        // Phase plot tab (available when 2+ variables exist)
        List<String> varNames = PhasePlotPane.getVariableNames(result);
        if (varNames.size() >= 2) {
            PhasePlotPane phasePlot = new PhasePlotPane(result, ghosts);
            phasePlotTab = ensureTab(phasePlotTab, "Phase Plot", phasePlot);
            resultTabs.getSelectionModel().select(simulationTab);
        }

        // Build ghost entry for next run's overlay
        runCounter++;
        Map<String, Double> previousParams = runHistory.isEmpty()
                ? Map.of()
                : runHistory.getLast().parameters();
        String name = GhostRun.generateName(runCounter, parameters, previousParams);
        int colorIndex = (runCounter - 1) % ChartUtils.GHOST_COLORS.size();
        runHistory.add(new GhostRun(result, name, colorIndex, parameters));
        if (runHistory.size() > MAX_HISTORY) {
            runHistory.removeFirst();
        }
    }

    public void showSweepResult(SweepResult result, String paramName) {
        clearStale();
        SweepResultPane pane = new SweepResultPane(result, paramName);
        sweepTab = ensureTab(sweepTab, "Sweep", pane);
        resultTabs.getSelectionModel().select(sweepTab);
    }

    public void showMonteCarloResult(MonteCarloResult result) {
        clearStale();
        MonteCarloResultPane pane = new MonteCarloResultPane(result);
        monteCarloTab = ensureTab(monteCarloTab, "Monte Carlo", pane);
        resultTabs.getSelectionModel().select(monteCarloTab);
    }

    public void showOptimizationResult(OptimizationResult result) {
        clearStale();
        OptimizationResultPane pane = new OptimizationResultPane(result);
        optimizationTab = ensureTab(optimizationTab, "Optimization", pane);
        resultTabs.getSelectionModel().select(optimizationTab);
    }

    public void showCalibrationResult(OptimizationResult result,
                                      List<CalibrateDialog.FitTarget> fitTargets) {
        clearStale();
        CalibrationResultPane pane = new CalibrationResultPane(result, fitTargets);
        calibrationTab = ensureTab(calibrationTab, "Calibration", pane);
        resultTabs.getSelectionModel().select(calibrationTab);
    }

    public void showMultiSweepResult(MultiSweepResult result) {
        clearStale();
        MultiSweepResultPane pane = new MultiSweepResultPane(result);
        multiSweepTab = ensureTab(multiSweepTab, "Multi-Sweep", pane);
        resultTabs.getSelectionModel().select(multiSweepTab);
    }

    public void showSensitivity(SensitivityPane pane) {
        clearStale();
        sensitivityTab = ensureTab(sensitivityTab, "Sensitivity", pane);
        resultTabs.getSelectionModel().select(sensitivityTab);
    }

    public void showLoopDominance(LoopDominanceAnalysis dominance) {
        if (dominance == null) {
            return;
        }
        LoopDominancePane pane = new LoopDominancePane(dominance);
        unbindCursors();
        dominanceCursor = pane.cursorTimeStepProperty();
        bindCursors();
        dominanceTab = ensureTab(dominanceTab, "Loop Dominance", pane);
        resultTabs.getSelectionModel().select(dominanceTab);
    }

    /**
     * Marks results as stale by showing an amber banner and border.
     * Only has an effect when result tabs are currently visible.
     */
    public void markStale() {
        if (stale || !resultTabs.isVisible()) {
            return;
        }
        stale = true;
        staleBanner.setVisible(true);
        staleBanner.setManaged(true);
        resultTabs.setStyle(STALE_BORDER_STYLE);
        if (dashboardTab != null) {
            dashboardTab.setGraphic(new Circle(STALE_DOT_RADIUS, STALE_DOT_COLOR));
        }
    }

    /**
     * Removes the stale indicator, restoring normal result display.
     */
    public void clearStale() {
        if (!stale) {
            return;
        }
        stale = false;
        staleBanner.setVisible(false);
        staleBanner.setManaged(false);
        resultTabs.setStyle("");
        if (dashboardTab != null) {
            dashboardTab.setGraphic(null);
        }
    }

    /**
     * Returns whether results are currently marked as stale.
     */
    public boolean isStale() {
        return stale;
    }

    /**
     * Returns whether the dashboard currently has any result tabs.
     */
    public boolean hasResults() {
        return !resultTabs.getTabs().isEmpty();
    }

    private Tab ensureTab(Tab existing, String title, Node content) {
        showTabs();
        if (existing != null && resultTabs.getTabs().contains(existing)) {
            existing.setContent(content);
            return existing;
        }
        Tab tab = new Tab(title, content);
        tab.setOnClosed(e -> {
            if (tab == simulationTab) {
                simulationTab = null;
            } else if (tab == sweepTab) {
                sweepTab = null;
            } else if (tab == monteCarloTab) {
                monteCarloTab = null;
            } else if (tab == optimizationTab) {
                optimizationTab = null;
            } else if (tab == calibrationTab) {
                calibrationTab = null;
            } else if (tab == multiSweepTab) {
                multiSweepTab = null;
            } else if (tab == sensitivityTab) {
                sensitivityTab = null;
            } else if (tab == dominanceTab) {
                dominanceTab = null;
            } else if (tab == phasePlotTab) {
                phasePlotTab = null;
            }
            if (resultTabs.getTabs().isEmpty()) {
                hideTabs();
            }
        });
        resultTabs.getTabs().add(tab);
        return tab;
    }

    void clearRunHistory() {
        runHistory.clear();
    }

    /**
     * Returns the number of previous simulation runs stored for ghost overlays.
     */
    int getRunHistorySize() {
        return runHistory.size();
    }

    /**
     * Removes all result tabs and shows the placeholder. Called when a new model is loaded.
     */
    public void clear() {
        unbindCursors();
        simulationCursor = null;
        dominanceCursor = null;
        runHistory.clear();
        runCounter = 0;
        stale = false;
        staleBanner.setVisible(false);
        staleBanner.setManaged(false);
        resultTabs.setStyle("");
        if (dashboardTab != null) {
            dashboardTab.setGraphic(null);
        }
        resultTabs.getTabs().clear();
        simulationTab = null;
        sweepTab = null;
        monteCarloTab = null;
        optimizationTab = null;
        calibrationTab = null;
        multiSweepTab = null;
        sensitivityTab = null;
        phasePlotTab = null;
        dominanceTab = null;
        hideTabs();
    }

    private void bindCursors() {
        if (simulationCursor == null || dominanceCursor == null) {
            return;
        }
        simToDomListener = (obs, oldVal, newVal) -> {
            if (!updatingCursor) {
                updatingCursor = true;
                try {
                    dominanceCursor.set(newVal.doubleValue());
                } finally {
                    updatingCursor = false;
                }
            }
        };
        domToSimListener = (obs, oldVal, newVal) -> {
            if (!updatingCursor) {
                updatingCursor = true;
                try {
                    simulationCursor.set(newVal.doubleValue());
                } finally {
                    updatingCursor = false;
                }
            }
        };
        simulationCursor.addListener(simToDomListener);
        dominanceCursor.addListener(domToSimListener);
    }

    private void unbindCursors() {
        if (simulationCursor != null && simToDomListener != null) {
            simulationCursor.removeListener(simToDomListener);
        }
        if (dominanceCursor != null && domToSimListener != null) {
            dominanceCursor.removeListener(domToSimListener);
        }
        simToDomListener = null;
        domToSimListener = null;
    }

    private void showTabs() {
        placeholder.setVisible(false);
        placeholder.setManaged(false);
        resultTabs.setVisible(true);
        resultTabs.setManaged(true);
    }

    private void hideTabs() {
        resultTabs.setVisible(false);
        resultTabs.setManaged(false);
        placeholder.setVisible(true);
        placeholder.setManaged(true);
    }
}

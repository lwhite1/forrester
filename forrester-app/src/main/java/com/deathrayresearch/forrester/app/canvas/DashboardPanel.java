package com.deathrayresearch.forrester.app.canvas;

import com.deathrayresearch.forrester.sweep.MonteCarloResult;
import com.deathrayresearch.forrester.sweep.OptimizationResult;
import com.deathrayresearch.forrester.sweep.SweepResult;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Dashboard panel that displays results from simulation, parameter sweep,
 * Monte Carlo, and optimization runs. Results are shown in separate tabs,
 * created on demand.
 */
public class DashboardPanel extends VBox {

    private final TabPane resultTabs;
    private final StackPane placeholder;
    private Tab simulationTab;
    private Tab sweepTab;
    private Tab monteCarloTab;
    private Tab optimizationTab;

    public DashboardPanel() {
        Label placeholderLabel = new Label("Run a simulation to see results.");
        placeholderLabel.setStyle(Styles.PLACEHOLDER_TEXT);
        placeholder = new StackPane(placeholderLabel);

        resultTabs = new TabPane();
        resultTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.SELECTED_TAB);
        resultTabs.setVisible(false);
        resultTabs.setManaged(false);

        VBox.setVgrow(placeholder, javafx.scene.layout.Priority.ALWAYS);
        VBox.setVgrow(resultTabs, javafx.scene.layout.Priority.ALWAYS);

        getChildren().addAll(placeholder, resultTabs);
    }

    public void showSimulationResult(SimulationRunner.SimulationResult result) {
        SimulationResultPane pane = new SimulationResultPane(result);
        simulationTab = ensureTab(simulationTab, "Simulation", pane);
        resultTabs.getSelectionModel().select(simulationTab);
    }

    public void showSweepResult(SweepResult result, String paramName) {
        SweepResultPane pane = new SweepResultPane(result, paramName);
        sweepTab = ensureTab(sweepTab, "Sweep", pane);
        resultTabs.getSelectionModel().select(sweepTab);
    }

    public void showMonteCarloResult(MonteCarloResult result) {
        MonteCarloResultPane pane = new MonteCarloResultPane(result);
        monteCarloTab = ensureTab(monteCarloTab, "Monte Carlo", pane);
        resultTabs.getSelectionModel().select(monteCarloTab);
    }

    public void showOptimizationResult(OptimizationResult result) {
        OptimizationResultPane pane = new OptimizationResultPane(result);
        optimizationTab = ensureTab(optimizationTab, "Optimization", pane);
        resultTabs.getSelectionModel().select(optimizationTab);
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
            }
            if (resultTabs.getTabs().isEmpty()) {
                hideTabs();
            }
        });
        resultTabs.getTabs().add(tab);
        return tab;
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

package com.deathrayresearch.forrester.app.canvas;

import com.deathrayresearch.forrester.sweep.MonteCarloResult;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

import java.util.ArrayList;
import java.util.List;

/**
 * Displays Monte Carlo results as a fan chart with a ComboBox for selecting
 * which stock or variable to visualize.
 */
public class MonteCarloResultPane extends BorderPane {

    private final MonteCarloResult result;
    private FanChartPane fanChartPane;

    public MonteCarloResultPane(MonteCarloResult result) {
        this.result = result;

        List<String> allNames = new ArrayList<>();
        allNames.addAll(result.getStockNames());
        allNames.addAll(result.getVariableNames());

        ComboBox<String> varCombo = new ComboBox<>(FXCollections.observableArrayList(allNames));

        HBox topBar = new HBox(8, new Label("Variable:"), varCombo);
        topBar.setPadding(new Insets(8));
        setTop(topBar);

        varCombo.valueProperty().addListener((obs, old, val) -> {
            if (val != null) {
                showVariable(val);
            }
        });

        if (!allNames.isEmpty()) {
            varCombo.setValue(allNames.get(0));
        }
    }

    private void showVariable(String variableName) {
        if (fanChartPane == null) {
            fanChartPane = new FanChartPane(result, variableName);
            setCenter(fanChartPane);
        } else {
            fanChartPane.redraw(result, variableName);
        }
    }
}

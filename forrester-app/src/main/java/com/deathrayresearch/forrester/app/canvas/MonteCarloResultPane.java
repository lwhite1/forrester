package com.deathrayresearch.forrester.app.canvas;

import com.deathrayresearch.forrester.sweep.MonteCarloResult;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Displays Monte Carlo results as a fan chart with a ComboBox for selecting
 * which stock or variable to visualize.
 */
public class MonteCarloResultPane extends BorderPane {

    private final MonteCarloResult result;
    private FanChartPane fanChartPane;
    private String currentVariable;

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
                currentVariable = val;
                showVariable(val);
            }
        });

        if (!allNames.isEmpty()) {
            varCombo.setValue(allNames.get(0));
        }

        ContextMenu contextMenu = new ContextMenu();
        MenuItem exportCsv = new MenuItem("Export CSV (Percentiles)...");
        exportCsv.setOnAction(e -> exportPercentileCsv());
        contextMenu.getItems().add(exportCsv);
        setOnContextMenuRequested(e ->
                contextMenu.show(this, e.getScreenX(), e.getScreenY()));
    }

    private void showVariable(String variableName) {
        if (fanChartPane == null) {
            fanChartPane = new FanChartPane(result, variableName);
            setCenter(fanChartPane);
        } else {
            fanChartPane.redraw(result, variableName);
        }
    }

    private void exportPercentileCsv() {
        if (currentVariable == null) {
            return;
        }
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Percentile CSV");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        fileChooser.setInitialFileName("montecarlo_percentiles.csv");

        File file = fileChooser.showSaveDialog(getScene() != null ? getScene().getWindow() : null);
        if (file != null) {
            try {
                result.writePercentileCsv(file.getAbsolutePath(), currentVariable,
                        2.5, 25, 50, 75, 97.5);
            } catch (RuntimeException e) {
                new Alert(Alert.AlertType.ERROR,
                        "Failed to export CSV: " + e.getMessage()).showAndWait();
            }
        }
    }
}

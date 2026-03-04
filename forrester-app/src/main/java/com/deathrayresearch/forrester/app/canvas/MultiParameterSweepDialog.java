package com.deathrayresearch.forrester.app.canvas;

import com.deathrayresearch.forrester.sweep.ParameterSweep;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Dialog for configuring a multi-parameter sweep. Users add rows for each
 * parameter to sweep, specifying start, end, and step values. The dialog
 * shows a live combination count and validates that at least 2 parameters
 * are configured with valid ranges.
 */
public class MultiParameterSweepDialog extends Dialog<MultiParameterSweepDialog.Config> {

    public record ParamConfig(String name, double start, double end, double step) {
    }

    public record Config(List<ParamConfig> parameters) {
    }

    private static final int MAX_COMBINATIONS = 10_000;
    private static final int WARN_COMBINATIONS = 1_000;

    private final ObservableList<ParameterRow> parameterRows = FXCollections.observableArrayList();
    private final Label combinationCountLabel;

    public MultiParameterSweepDialog(List<String> constantNames) {
        setTitle("Multi-Parameter Sweep");
        setHeaderText("Configure parameters to sweep (at least 2)");

        VBox paramBox = new VBox(6);
        paramBox.setPadding(new Insets(5));

        combinationCountLabel = new Label("0 combinations");
        combinationCountLabel.setId("multiSweepCombinationCount");
        combinationCountLabel.setPadding(new Insets(4, 0, 4, 0));

        Button addButton = new Button("Add Parameter");
        addButton.setId("multiSweepAddParam");
        addButton.setOnAction(e -> {
            ParameterRow row = new ParameterRow(constantNames, paramBox, null);
            parameterRows.add(row);
            paramBox.getChildren().add(paramBox.getChildren().size() - 2, row.getPane());
            updateCombinationCount();
        });

        paramBox.getChildren().addAll(combinationCountLabel, addButton);

        // Add two default rows
        for (int i = 0; i < 2; i++) {
            String defaultName = constantNames.size() > i ? constantNames.get(i) : null;
            ParameterRow row = new ParameterRow(constantNames, paramBox, defaultName);
            parameterRows.add(row);
            paramBox.getChildren().add(paramBox.getChildren().size() - 2, row.getPane());
        }
        updateCombinationCount();

        ScrollPane paramScroll = new ScrollPane(paramBox);
        paramScroll.setFitToWidth(true);
        paramScroll.setPrefHeight(250);

        Label paramsLabel = new Label("Parameters");
        paramsLabel.setStyle(Styles.SECTION_HEADER);

        VBox content = new VBox(10, paramsLabel, paramScroll);
        content.setPadding(new Insets(10));
        getDialogPane().setContent(content);
        getDialogPane().setPrefWidth(560);

        ButtonType okButton = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(okButton, ButtonType.CANCEL);

        getDialogPane().lookupButton(okButton).disableProperty().bind(
                Bindings.createBooleanBinding(() -> getValidParams().size() < 2,
                        parameterRows)
        );

        Button okNode = (Button) getDialogPane().lookupButton(okButton);
        okNode.addEventFilter(ActionEvent.ACTION, event -> {
            List<ParamConfig> validParams = getValidParams();
            if (validParams.size() < 2) {
                event.consume();
                new Alert(Alert.AlertType.WARNING,
                        "At least 2 valid parameter rows are required.").showAndWait();
                return;
            }
            Set<String> names = new HashSet<>();
            for (ParamConfig p : validParams) {
                if (!names.add(p.name())) {
                    event.consume();
                    new Alert(Alert.AlertType.WARNING,
                            "Duplicate parameter: " + p.name()
                                    + ". Each parameter must be unique.").showAndWait();
                    return;
                }
            }
            long combos = computeCombinations(validParams);
            if (combos > MAX_COMBINATIONS) {
                event.consume();
                new Alert(Alert.AlertType.WARNING,
                        "Too many combinations (" + combos + "). Maximum is "
                                + MAX_COMBINATIONS + ".").showAndWait();
            }
        });

        setResultConverter(button -> {
            if (button == okButton) {
                return new Config(getValidParams());
            }
            return null;
        });
    }

    private List<ParamConfig> getValidParams() {
        List<ParamConfig> params = new ArrayList<>();
        for (ParameterRow row : parameterRows) {
            if (row.isValid()) {
                params.add(row.toConfig());
            }
        }
        return params;
    }

    private void updateCombinationCount() {
        List<ParamConfig> validParams = getValidParams();
        long combos = computeCombinations(validParams);
        combinationCountLabel.setText(combos + " combinations");
        if (combos > MAX_COMBINATIONS) {
            combinationCountLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
        } else if (combos > WARN_COMBINATIONS) {
            combinationCountLabel.setStyle("-fx-text-fill: #cc7700; -fx-font-weight: bold;");
        } else {
            combinationCountLabel.setStyle("-fx-font-weight: bold;");
        }
    }

    private static long computeCombinations(List<ParamConfig> params) {
        if (params.isEmpty()) {
            return 0;
        }
        long product = 1;
        for (ParamConfig p : params) {
            int count = ParameterSweep.linspace(p.start(), p.end(), p.step()).length;
            product *= count;
            if (product > 10_000_000) {
                return product;
            }
        }
        return product;
    }

    private class ParameterRow {
        private final ComboBox<String> nameCombo;
        private final TextField startField;
        private final TextField endField;
        private final TextField stepField;
        private final HBox pane;

        ParameterRow(List<String> constantNames, VBox container, String defaultName) {
            int rowIndex = parameterRows.size();

            nameCombo = new ComboBox<>(FXCollections.observableArrayList(constantNames));
            if (defaultName != null) {
                nameCombo.setValue(defaultName);
            } else if (!constantNames.isEmpty()) {
                nameCombo.setValue(constantNames.get(0));
            }
            nameCombo.setPrefWidth(130);
            nameCombo.setId("multiSweepParamName" + rowIndex);

            startField = new TextField("0");
            startField.setPrefWidth(70);
            startField.setPromptText("Start");
            startField.setId("multiSweepStart" + rowIndex);

            endField = new TextField("10");
            endField.setPrefWidth(70);
            endField.setPromptText("End");
            endField.setId("multiSweepEnd" + rowIndex);

            stepField = new TextField("1");
            stepField.setPrefWidth(70);
            stepField.setPromptText("Step");
            stepField.setId("multiSweepStep" + rowIndex);

            startField.textProperty().addListener((obs, o, n) -> updateCombinationCount());
            endField.textProperty().addListener((obs, o, n) -> updateCombinationCount());
            stepField.textProperty().addListener((obs, o, n) -> updateCombinationCount());

            Button removeBtn = new Button("X");

            pane = new HBox(6,
                    nameCombo,
                    new Label("Start:"), startField,
                    new Label("End:"), endField,
                    new Label("Step:"), stepField,
                    removeBtn);
            pane.setPadding(new Insets(2));

            removeBtn.setOnAction(e -> {
                parameterRows.remove(this);
                container.getChildren().remove(pane);
                updateCombinationCount();
            });
        }

        HBox getPane() {
            return pane;
        }

        boolean isValid() {
            if (nameCombo.getValue() == null) {
                return false;
            }
            try {
                double start = Double.parseDouble(startField.getText().trim());
                double end = Double.parseDouble(endField.getText().trim());
                double step = Double.parseDouble(stepField.getText().trim());
                return Double.isFinite(start) && Double.isFinite(end)
                        && Double.isFinite(step) && start <= end && step > 0;
            } catch (NumberFormatException e) {
                return false;
            }
        }

        ParamConfig toConfig() {
            return new ParamConfig(
                    nameCombo.getValue(),
                    Double.parseDouble(startField.getText().trim()),
                    Double.parseDouble(endField.getText().trim()),
                    Double.parseDouble(stepField.getText().trim()));
        }
    }
}

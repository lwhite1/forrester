package com.deathrayresearch.forrester.app.canvas;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.event.ActionEvent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/**
 * Dialog for configuring a Monte Carlo simulation: add parameters with
 * distribution types, set iteration count, sampling method, and seed.
 */
public class MonteCarloDialog extends Dialog<MonteCarloDialog.Config> {

    public enum DistributionType {
        NORMAL, UNIFORM
    }

    public record ParameterConfig(String name, DistributionType distribution,
                                  double param1, double param2) {
    }

    public record Config(
            List<ParameterConfig> parameters,
            int iterations,
            String samplingMethod,
            long seed
    ) {
    }

    private final ObservableList<ParameterRow> parameterRows = FXCollections.observableArrayList();
    private final List<String> constantNames;
    private final TextField iterationsField;
    private final ComboBox<String> samplingCombo;
    private final TextField seedField;

    public MonteCarloDialog(List<String> constantNames) {
        this.constantNames = constantNames;
        setTitle("Monte Carlo Simulation");
        setHeaderText("Configure Monte Carlo parameters");

        VBox paramBox = new VBox(6);
        paramBox.setPadding(new Insets(5));

        Button addButton = new Button("Add Parameter");
        addButton.setOnAction(e -> {
            ParameterRow row = new ParameterRow(constantNames, paramBox);
            parameterRows.add(row);
            paramBox.getChildren().add(paramBox.getChildren().size() - 1, row.getPane());
        });

        paramBox.getChildren().add(addButton);

        // Add one default row
        ParameterRow defaultRow = new ParameterRow(constantNames, paramBox);
        parameterRows.add(defaultRow);
        paramBox.getChildren().add(0, defaultRow.getPane());

        ScrollPane paramScroll = new ScrollPane(paramBox);
        paramScroll.setFitToWidth(true);
        paramScroll.setPrefHeight(200);

        iterationsField = new TextField("200");
        samplingCombo = new ComboBox<>(FXCollections.observableArrayList(
                "LATIN_HYPERCUBE", "RANDOM"));
        samplingCombo.setValue("LATIN_HYPERCUBE");
        seedField = new TextField("12345");

        GridPane settingsGrid = new GridPane();
        settingsGrid.setHgap(10);
        settingsGrid.setVgap(10);
        settingsGrid.setPadding(new Insets(10, 0, 0, 0));

        settingsGrid.add(new Label("Iterations:"), 0, 0);
        settingsGrid.add(iterationsField, 1, 0);
        settingsGrid.add(new Label("Sampling:"), 0, 1);
        settingsGrid.add(samplingCombo, 1, 1);
        settingsGrid.add(new Label("Seed:"), 0, 2);
        settingsGrid.add(seedField, 1, 2);

        Label paramsLabel = new Label("Parameters");
        paramsLabel.setStyle(Styles.SECTION_HEADER);

        VBox content = new VBox(10, paramsLabel, paramScroll, settingsGrid);
        content.setPadding(new Insets(10));
        getDialogPane().setContent(content);
        getDialogPane().setPrefWidth(500);

        ButtonType okButton = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(okButton, ButtonType.CANCEL);

        getDialogPane().lookupButton(okButton).disableProperty().bind(
                Bindings.createBooleanBinding(this::isInvalid,
                        iterationsField.textProperty(), seedField.textProperty())
        );

        Button okNode = (Button) getDialogPane().lookupButton(okButton);
        okNode.addEventFilter(ActionEvent.ACTION, event -> {
            boolean hasValid = parameterRows.stream().anyMatch(ParameterRow::isValid);
            if (!hasValid) {
                event.consume();
                new Alert(Alert.AlertType.WARNING,
                        "At least one parameter row must have valid values.").showAndWait();
            }
        });

        setResultConverter(button -> {
            if (button == okButton) {
                List<ParameterConfig> params = new ArrayList<>();
                for (ParameterRow row : parameterRows) {
                    if (row.isValid()) {
                        params.add(row.toConfig());
                    }
                }
                return new Config(
                        params,
                        Integer.parseInt(iterationsField.getText().trim()),
                        samplingCombo.getValue(),
                        Long.parseLong(seedField.getText().trim())
                );
            }
            return null;
        });
    }

    private boolean isInvalid() {
        try {
            int iter = Integer.parseInt(iterationsField.getText().trim());
            Long.parseLong(seedField.getText().trim());
            return iter < 1;
        } catch (NumberFormatException e) {
            return true;
        }
    }

    private class ParameterRow {
        private final ComboBox<String> nameCombo;
        private final ComboBox<DistributionType> distCombo;
        private final TextField param1Field;
        private final TextField param2Field;
        private final Label param1Label;
        private final Label param2Label;
        private final HBox pane;

        ParameterRow(List<String> constantNames, VBox container) {
            nameCombo = new ComboBox<>(FXCollections.observableArrayList(constantNames));
            if (!constantNames.isEmpty()) {
                nameCombo.setValue(constantNames.get(0));
            }
            nameCombo.setPrefWidth(130);

            distCombo = new ComboBox<>(FXCollections.observableArrayList(DistributionType.values()));
            distCombo.setValue(DistributionType.NORMAL);
            distCombo.setPrefWidth(100);

            param1Label = new Label("Mean:");
            param1Field = new TextField("0");
            param1Field.setPrefWidth(60);
            param2Label = new Label("StdDev:");
            param2Field = new TextField("1");
            param2Field.setPrefWidth(60);

            distCombo.valueProperty().addListener((obs, old, val) -> updateLabels(val));

            pane = new HBox(6);
            pane.setPadding(new Insets(2));

            Button removeBtn = new Button("X");
            removeBtn.setOnAction(e -> {
                parameterRows.remove(this);
                container.getChildren().remove(pane);
            });

            pane.getChildren().addAll(nameCombo, distCombo, param1Label, param1Field,
                    param2Label, param2Field, removeBtn);
        }

        private void updateLabels(DistributionType type) {
            if (type == DistributionType.UNIFORM) {
                param1Label.setText("Min:");
                param2Label.setText("Max:");
            } else {
                param1Label.setText("Mean:");
                param2Label.setText("StdDev:");
            }
        }

        HBox getPane() {
            return pane;
        }

        boolean isValid() {
            if (nameCombo.getValue() == null) {
                return false;
            }
            try {
                Double.parseDouble(param1Field.getText().trim());
                double p2 = Double.parseDouble(param2Field.getText().trim());
                if (distCombo.getValue() == DistributionType.NORMAL && p2 <= 0) {
                    return false;
                }
                if (distCombo.getValue() == DistributionType.UNIFORM) {
                    double p1 = Double.parseDouble(param1Field.getText().trim());
                    return p1 < p2;
                }
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }

        ParameterConfig toConfig() {
            return new ParameterConfig(
                    nameCombo.getValue(),
                    distCombo.getValue(),
                    Double.parseDouble(param1Field.getText().trim()),
                    Double.parseDouble(param2Field.getText().trim())
            );
        }
    }
}

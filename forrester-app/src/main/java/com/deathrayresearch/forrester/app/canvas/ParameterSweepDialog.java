package com.deathrayresearch.forrester.app.canvas;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import java.util.List;

/**
 * Dialog for configuring a parameter sweep: select a constant to sweep,
 * specify start/end/step values, and choose which variable to track.
 */
public class ParameterSweepDialog extends Dialog<ParameterSweepDialog.Config> {

    public record Config(
            String parameterName,
            double start,
            double end,
            double step,
            String trackVariable
    ) {
    }

    private final ComboBox<String> parameterCombo;
    private final TextField startField;
    private final TextField endField;
    private final TextField stepField;
    private final ComboBox<String> trackCombo;

    public ParameterSweepDialog(List<String> constantNames, List<String> trackableNames) {
        setTitle("Parameter Sweep");
        setHeaderText("Configure parameter sweep");

        parameterCombo = new ComboBox<>(FXCollections.observableArrayList(constantNames));
        startField = new TextField("0");
        endField = new TextField("10");
        stepField = new TextField("1");
        trackCombo = new ComboBox<>(FXCollections.observableArrayList(trackableNames));

        if (!constantNames.isEmpty()) {
            parameterCombo.setValue(constantNames.get(0));
        }
        if (!trackableNames.isEmpty()) {
            trackCombo.setValue(trackableNames.get(0));
        }

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 10, 10));

        grid.add(new Label("Parameter:"), 0, 0);
        grid.add(parameterCombo, 1, 0);
        grid.add(new Label("Start:"), 0, 1);
        grid.add(startField, 1, 1);
        grid.add(new Label("End:"), 0, 2);
        grid.add(endField, 1, 2);
        grid.add(new Label("Step:"), 0, 3);
        grid.add(stepField, 1, 3);
        grid.add(new Label("Track:"), 0, 4);
        grid.add(trackCombo, 1, 4);

        getDialogPane().setContent(grid);

        ButtonType okButton = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(okButton, ButtonType.CANCEL);

        getDialogPane().lookupButton(okButton).disableProperty().bind(
                Bindings.createBooleanBinding(this::isInvalid,
                        startField.textProperty(), endField.textProperty(),
                        stepField.textProperty(),
                        parameterCombo.valueProperty(), trackCombo.valueProperty())
        );

        setResultConverter(button -> {
            if (button == okButton) {
                return new Config(
                        parameterCombo.getValue(),
                        Double.parseDouble(startField.getText().trim()),
                        Double.parseDouble(endField.getText().trim()),
                        Double.parseDouble(stepField.getText().trim()),
                        trackCombo.getValue()
                );
            }
            return null;
        });
    }

    private boolean isInvalid() {
        if (parameterCombo.getValue() == null || trackCombo.getValue() == null) {
            return true;
        }
        try {
            double start = Double.parseDouble(startField.getText().trim());
            double end = Double.parseDouble(endField.getText().trim());
            double step = Double.parseDouble(stepField.getText().trim());
            if (start >= end || step <= 0 || !Double.isFinite(start)
                    || !Double.isFinite(end) || !Double.isFinite(step)) {
                return true;
            }
            long pointCount = (long) Math.ceil((end - start) / step) + 1;
            return pointCount > 10_000;
        } catch (NumberFormatException e) {
            return true;
        }
    }
}

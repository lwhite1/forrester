package systems.courant.sd.app.canvas;

import systems.courant.sd.model.def.SimulationSettings;

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

/**
 * Dialog for configuring simulation settings: time step unit, duration amount,
 * and duration unit.
 */
public class SimulationSettingsDialog extends Dialog<SimulationSettings> {

    private static final String[] TIME_UNIT_OPTIONS = {
            "Day", "Week", "Month", "Year", "Hour", "Minute", "Second"
    };

    private final ComboBox<String> timeStepCombo;
    private final TextField durationField;
    private final ComboBox<String> durationUnitCombo;
    private final TextField dtField;

    public SimulationSettingsDialog(SimulationSettings existing) {
        setTitle("Simulation Settings");
        setHeaderText("Configure simulation parameters");

        timeStepCombo = new ComboBox<>(FXCollections.observableArrayList(TIME_UNIT_OPTIONS));
        timeStepCombo.setId("simTimeStep");
        durationField = new TextField();
        durationField.setId("simDuration");
        durationUnitCombo = new ComboBox<>(FXCollections.observableArrayList(TIME_UNIT_OPTIONS));
        durationUnitCombo.setId("simDurationUnit");
        dtField = new TextField();
        dtField.setId("simDt");
        dtField.setPromptText("e.g. 0.25");

        if (existing != null) {
            timeStepCombo.setValue(existing.timeStep());
            durationField.setText(formatDuration(existing.duration()));
            durationUnitCombo.setValue(existing.durationUnit());
            dtField.setText(formatDuration(existing.dt()));
        } else {
            timeStepCombo.setValue("Day");
            durationField.setText("100");
            durationUnitCombo.setValue("Day");
            dtField.setText("1");
        }

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 10, 10));

        grid.add(new Label("Time Step:"), 0, 0);
        grid.add(timeStepCombo, 1, 0);
        grid.add(new Label("Duration:"), 0, 1);
        grid.add(durationField, 1, 1);
        grid.add(new Label("Duration Unit:"), 0, 2);
        grid.add(durationUnitCombo, 1, 2);
        grid.add(new Label("DT:"), 0, 3);
        grid.add(dtField, 1, 3);

        getDialogPane().setContent(grid);

        ButtonType okButton = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(okButton, ButtonType.CANCEL);

        // Validate: disable OK when duration or DT is not a positive number
        getDialogPane().lookupButton(okButton).disableProperty().bind(
                Bindings.createBooleanBinding(
                        () -> !isValidPositiveNumber(durationField.getText())
                                || !isValidPositiveNumber(dtField.getText()),
                        durationField.textProperty(),
                        dtField.textProperty()
                )
        );

        setResultConverter(button -> {
            if (button == okButton) {
                return new SimulationSettings(
                        timeStepCombo.getValue(),
                        Double.parseDouble(durationField.getText().trim()),
                        durationUnitCombo.getValue(),
                        Double.parseDouble(dtField.getText().trim())
                );
            }
            return null;
        });
    }

    private static String formatDuration(double value) {
        if (value == Math.floor(value) && Double.isFinite(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }

    private static boolean isValidPositiveNumber(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        try {
            double value = Double.parseDouble(text.trim());
            return value > 0 && Double.isFinite(value);
        } catch (NumberFormatException e) {
            return false;
        }
    }
}

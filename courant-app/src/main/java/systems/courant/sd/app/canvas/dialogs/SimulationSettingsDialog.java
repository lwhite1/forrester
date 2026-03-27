package systems.courant.sd.app.canvas.dialogs;

import systems.courant.sd.model.def.SimulationSettings;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

/**
 * Dialog for configuring simulation settings: time step unit, duration amount,
 * duration unit, strict mode, and save-per recording interval.
 */
public class SimulationSettingsDialog extends ValidatingDialog<SimulationSettings> {

    private static final String[] TIME_UNIT_OPTIONS = {
            "Day", "Week", "Month", "Year", "Hour", "Minute", "Second"
    };

    private final ComboBox<String> timeStepCombo;
    private final TextField durationField;
    private final ComboBox<String> durationUnitCombo;
    private final TextField dtField;
    private final CheckBox strictModeCheckBox;
    private final TextField savePerField;

    public SimulationSettingsDialog(SimulationSettings existing) {
        super("Simulation Settings", "Configure simulation parameters");

        timeStepCombo = new ComboBox<>(FXCollections.observableArrayList(TIME_UNIT_OPTIONS));
        timeStepCombo.setId("simTimeStep");
        durationField = new TextField();
        durationField.setId("simDuration");
        durationUnitCombo = new ComboBox<>(FXCollections.observableArrayList(TIME_UNIT_OPTIONS));
        durationUnitCombo.setId("simDurationUnit");
        dtField = new TextField();
        dtField.setId("simDt");
        dtField.setPromptText("e.g. 0.25");
        strictModeCheckBox = new CheckBox("Fail fast on NaN / Infinity");
        strictModeCheckBox.setId("simStrictMode");
        savePerField = new TextField();
        savePerField.setId("simSavePer");
        savePerField.setPromptText("e.g. 10");

        if (existing != null) {
            timeStepCombo.setValue(existing.timeStep());
            durationField.setText(formatDuration(existing.duration()));
            durationUnitCombo.setValue(existing.durationUnit());
            dtField.setText(formatDuration(existing.dt()));
            strictModeCheckBox.setSelected(existing.strictMode());
            savePerField.setText(String.valueOf(existing.savePer()));
        } else {
            timeStepCombo.setValue("Day");
            durationField.setText("100");
            durationUnitCombo.setValue("Day");
            dtField.setText("1");
            strictModeCheckBox.setSelected(false);
            savePerField.setText("1");
        }

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 10, 10));

        // Time Step section
        grid.add(new Label("Time Step:"), 0, 0);
        grid.add(timeStepCombo, 1, 0);
        grid.add(new Label("DT:"), 0, 1);
        grid.add(dtField, 1, 1);
        grid.add(new Label("Save Per:"), 0, 2);
        grid.add(savePerField, 1, 2);

        Separator timeSep = new Separator();
        timeSep.setId("simTimeSeparator");
        grid.add(timeSep, 0, 3, 2, 1);

        // Duration section
        grid.add(new Label("Duration:"), 0, 4);
        grid.add(durationField, 1, 4);
        grid.add(new Label("Duration Unit:"), 0, 5);
        grid.add(durationUnitCombo, 1, 5);

        Separator durationSep = new Separator();
        durationSep.setId("simDurationSeparator");
        grid.add(durationSep, 0, 6, 2, 1);

        // Strict Mode (last)
        grid.add(new Label("Strict Mode:"), 0, 7);
        grid.add(strictModeCheckBox, 1, 7);

        setStandardContent(grid);

        // Validate: disable OK when duration, DT, or savePer is not valid
        bindOkDisable(Bindings.createBooleanBinding(
                () -> !isValidPositiveNumber(durationField.getText())
                        || !isValidPositiveNumber(dtField.getText())
                        || !isValidPositiveInteger(savePerField.getText()),
                durationField.textProperty(),
                dtField.textProperty(),
                savePerField.textProperty()
        ));
    }

    @Override
    protected SimulationSettings buildResult() {
        return new SimulationSettings(
                timeStepCombo.getValue(),
                Double.parseDouble(durationField.getText().trim()),
                durationUnitCombo.getValue(),
                Double.parseDouble(dtField.getText().trim()),
                strictModeCheckBox.isSelected(),
                Long.parseLong(savePerField.getText().trim())
        );
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

    private static boolean isValidPositiveInteger(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        try {
            long value = Long.parseLong(text.trim());
            return value >= 1;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}

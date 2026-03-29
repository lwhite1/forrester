package systems.courant.sd.app.canvas.dialogs;

import systems.courant.sd.Simulation;
import systems.courant.sd.measure.units.time.TimeUnits;
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
 *
 * <p>When the user changes the Time Step unit, the Duration Unit is automatically
 * updated to match, reducing the risk of accidental mismatches that produce an
 * unmanageable number of simulation steps. The user can still manually override
 * the Duration Unit afterwards.
 *
 * <p>A step-count warning is shown when the selected settings would produce more
 * than {@value #WARNING_THRESHOLD} steps, and the OK button is disabled entirely
 * if the count would exceed {@link Simulation#MAX_STEPS}.
 */
public class SimulationSettingsDialog extends ValidatingDialog<SimulationSettings> {

    private static final String[] TIME_UNIT_OPTIONS = {
            "Day", "Week", "Month", "Year", "Hour", "Minute", "Second"
    };

    static final long WARNING_THRESHOLD = 100_000L;

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

        // Auto-sync Duration Unit when Time Step changes (listener added after
        // initial values are set so it does not override existing settings)
        timeStepCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                durationUnitCombo.setValue(newVal);
            }
        });

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

        // Step count warning (informational, does not block OK)
        Label warningLabel = new Label();
        warningLabel.setId("simStepWarning");
        warningLabel.setStyle("-fx-text-fill: #b35900; -fx-font-size: 11px;");
        warningLabel.setWrapText(true);
        warningLabel.setMaxWidth(Double.MAX_VALUE);
        warningLabel.managedProperty().bind(warningLabel.textProperty().isNotEmpty());
        warningLabel.visibleProperty().bind(warningLabel.textProperty().isNotEmpty());
        warningLabel.textProperty().bind(Bindings.createStringBinding(
                this::stepCountWarning,
                durationField.textProperty(), dtField.textProperty(),
                timeStepCombo.valueProperty(), durationUnitCombo.valueProperty()));
        grid.add(warningLabel, 0, 6, 2, 1);

        Separator durationSep = new Separator();
        durationSep.setId("simDurationSeparator");
        grid.add(durationSep, 0, 7, 2, 1);

        // Strict Mode
        grid.add(new Label("Strict Mode:"), 0, 8);
        grid.add(strictModeCheckBox, 1, 8);

        // Validation error (blocks OK when non-empty)
        Label validationLabel = bindValidation("simValidation", this::validate,
                durationField.textProperty(), dtField.textProperty(),
                savePerField.textProperty(), timeStepCombo.valueProperty(),
                durationUnitCombo.valueProperty());
        validationLabel.managedProperty().bind(validationLabel.textProperty().isNotEmpty());
        validationLabel.visibleProperty().bind(validationLabel.textProperty().isNotEmpty());
        grid.add(validationLabel, 0, 9, 2, 1);

        setStandardContent(grid);
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

    /**
     * Estimates the total number of simulation steps for the given settings.
     * Uses the same formula as {@link Simulation#execute()}.
     */
    static long estimateSteps(String timeStep, double duration, String durationUnit, double dt) {
        double timeStepRatio = TimeUnits.valueOf(timeStep.toUpperCase()).ratioToBaseUnit();
        double durationUnitRatio = TimeUnits.valueOf(durationUnit.toUpperCase()).ratioToBaseUnit();
        double rawSteps = (duration * durationUnitRatio) / (timeStepRatio * dt);
        double nearest = Math.rint(rawSteps);
        if (Math.abs(rawSteps - nearest) < 1e-9) {
            return (long) nearest;
        }
        return (long) Math.floor(rawSteps);
    }

    private String validate() {
        if (!isValidPositiveNumber(durationField.getText())
                || !isValidPositiveNumber(dtField.getText())
                || !isValidPositiveInteger(savePerField.getText())) {
            return "Duration, DT, and Save Per must be positive numbers.";
        }

        String tsUnit = timeStepCombo.getValue();
        String durUnit = durationUnitCombo.getValue();
        if (tsUnit == null || durUnit == null) {
            return "";
        }

        double duration = Double.parseDouble(durationField.getText().trim());
        double dt = Double.parseDouble(dtField.getText().trim());
        long steps = estimateSteps(tsUnit, duration, durUnit, dt);
        if (steps > Simulation.MAX_STEPS) {
            return String.format("Too many steps (%,d). Maximum is %,d. "
                    + "Increase Time Step or reduce Duration.", steps, Simulation.MAX_STEPS);
        }

        return "";
    }

    private String stepCountWarning() {
        if (!isValidPositiveNumber(durationField.getText())
                || !isValidPositiveNumber(dtField.getText())) {
            return "";
        }

        String tsUnit = timeStepCombo.getValue();
        String durUnit = durationUnitCombo.getValue();
        if (tsUnit == null || durUnit == null) {
            return "";
        }

        double duration = Double.parseDouble(durationField.getText().trim());
        double dt = Double.parseDouble(dtField.getText().trim());
        long steps = estimateSteps(tsUnit, duration, durUnit, dt);
        if (steps > WARNING_THRESHOLD && steps <= Simulation.MAX_STEPS) {
            return String.format("~%,d steps — simulation may be slow", steps);
        }

        return "";
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

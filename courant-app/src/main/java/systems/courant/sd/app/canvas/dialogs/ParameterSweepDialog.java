package systems.courant.sd.app.canvas.dialogs;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import java.util.List;

/**
 * Dialog for configuring a parameter sweep: select a constant to sweep,
 * specify start/end/step values, and choose which variable to track.
 */
public class ParameterSweepDialog extends ValidatingDialog<ParameterSweepDialog.Config> {

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

    public ParameterSweepDialog(List<String> constantNames, List<String> trackableNames,
                                Config previousConfig) {
        super("Parameter Sweep", "Configure parameter sweep");

        parameterCombo = new ComboBox<>(FXCollections.observableArrayList(constantNames));
        parameterCombo.setId("sweepParameter");
        startField = new TextField(previousConfig != null
                ? String.valueOf(previousConfig.start()) : "0");
        startField.setId("sweepStart");
        endField = new TextField(previousConfig != null
                ? String.valueOf(previousConfig.end()) : "10");
        endField.setId("sweepEnd");
        stepField = new TextField(previousConfig != null
                ? String.valueOf(previousConfig.step()) : "1");
        stepField.setId("sweepStep");
        trackCombo = new ComboBox<>(FXCollections.observableArrayList(trackableNames));
        trackCombo.setId("sweepTrack");

        if (previousConfig != null && constantNames.contains(previousConfig.parameterName())) {
            parameterCombo.setValue(previousConfig.parameterName());
        } else if (!constantNames.isEmpty()) {
            parameterCombo.setValue(constantNames.getFirst());
        }
        if (previousConfig != null && trackableNames.contains(previousConfig.trackVariable())) {
            trackCombo.setValue(previousConfig.trackVariable());
        } else if (!trackableNames.isEmpty()) {
            trackCombo.setValue(trackableNames.getFirst());
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

        Label validationLabel = bindValidation("sweepValidationLabel",
                this::getValidationMessage,
                startField.textProperty(), endField.textProperty(),
                stepField.textProperty(),
                parameterCombo.valueProperty(), trackCombo.valueProperty());
        grid.add(validationLabel, 0, 5, 2, 1);

        setStandardContent(grid);
    }

    @Override
    protected Config buildResult() {
        return new Config(
                parameterCombo.getValue(),
                Double.parseDouble(startField.getText().trim()),
                Double.parseDouble(endField.getText().trim()),
                Double.parseDouble(stepField.getText().trim()),
                trackCombo.getValue()
        );
    }

    private String getValidationMessage() {
        if (parameterCombo.getValue() == null) {
            return "Select a parameter to sweep.";
        }
        if (trackCombo.getValue() == null) {
            return "Select a variable to track.";
        }
        try {
            double start = Double.parseDouble(startField.getText().trim());
            double end = Double.parseDouble(endField.getText().trim());
            double step = Double.parseDouble(stepField.getText().trim());
            if (!Double.isFinite(start) || !Double.isFinite(end) || !Double.isFinite(step)) {
                return "Start, end, and step must be finite numbers.";
            }
            if (start > end) {
                return "Start must be less than or equal to end.";
            }
            if (step <= 0) {
                return "Step must be greater than zero.";
            }
            long pointCount = start == end ? 1
                    : Math.round((end - start) / step) + 1;
            if (pointCount > 10_000) {
                return "Too many points (" + pointCount + "). Maximum is 10,000.";
            }
            return "";
        } catch (NumberFormatException e) {
            return "Start, end, and step must be valid numbers.";
        }
    }
}

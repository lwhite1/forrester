package systems.courant.sd.app.canvas.dialogs;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

import systems.courant.sd.app.canvas.ParameterRowBase;
import systems.courant.sd.app.canvas.Styles;

/**
 * Dialog for configuring an optimization run: parameters with bounds,
 * objective type, target variable, algorithm, and max evaluations.
 */
public class OptimizerDialog extends ValidatingDialog<OptimizerDialog.Config> {

    public enum ObjectiveType {
        MINIMIZE, MAXIMIZE, TARGET, MINIMIZE_PEAK
    }

    public record ParamConfig(String name, double lower, double upper, double initialGuess) {
    }

    public record Config(
            List<ParamConfig> parameters,
            ObjectiveType objectiveType,
            String targetVariable,
            double targetValue,
            String algorithm,
            int maxEvaluations
    ) {
    }

    private final ObservableList<ParamRow> paramRows = FXCollections.observableArrayList();
    private final IntegerProperty fieldChangeCounter = new SimpleIntegerProperty(0);
    private ComboBox<ObjectiveType> objectiveCombo;
    private ComboBox<String> targetVarCombo;
    private TextField targetValueField;
    private Label targetValueLabel;
    private ComboBox<String> algorithmCombo;
    private TextField maxEvalsField;

    public OptimizerDialog(List<String> constantNames, List<String> stockNames) {
        super("Optimize", "Configure optimization");

        Label paramsLabel = new Label("Parameters");
        paramsLabel.setStyle(Styles.SECTION_HEADER);

        VBox content = new VBox(10,
                paramsLabel,
                buildParametersSection(constantNames),
                buildSettingsGrid(stockNames),
                bindValidation("optimizerValidationLabel", this::getValidationMessage,
                        maxEvalsField.textProperty(), targetValueField.textProperty(),
                        objectiveCombo.valueProperty(), targetVarCombo.valueProperty(),
                        paramRows, fieldChangeCounter));
        content.setPadding(new Insets(10));
        setStandardContent(content);
    }

    @Override
    protected Config buildResult() {
        List<ParamConfig> params = new ArrayList<>();
        for (ParamRow row : paramRows) {
            if (row.isValid()) {
                params.add(row.toConfig());
            }
        }
        double targetVal = 0;
        if (objectiveCombo.getValue() == ObjectiveType.TARGET) {
            targetVal = Double.parseDouble(targetValueField.getText().trim());
        }
        return new Config(
                params,
                objectiveCombo.getValue(),
                targetVarCombo.getValue(),
                targetVal,
                algorithmCombo.getValue(),
                Integer.parseInt(maxEvalsField.getText().trim())
        );
    }

    private ScrollPane buildParametersSection(List<String> constantNames) {
        VBox paramBox = new VBox(6);
        paramBox.setPadding(new Insets(5));

        Button addButton = new Button("Add Parameter");
        addButton.setId("optAddParam");
        addButton.setOnAction(e -> {
            ParamRow row = new ParamRow(constantNames, paramBox);
            paramRows.add(row);
            paramBox.getChildren().add(paramBox.getChildren().size() - 1, row.getPane());
        });

        paramBox.getChildren().add(addButton);

        ParamRow defaultRow = new ParamRow(constantNames, paramBox);
        paramRows.add(defaultRow);
        paramBox.getChildren().add(0, defaultRow.getPane());

        ScrollPane paramScroll = new ScrollPane(paramBox);
        paramScroll.setFitToWidth(true);
        paramScroll.setPrefHeight(150);
        return paramScroll;
    }

    private GridPane buildSettingsGrid(List<String> stockNames) {
        objectiveCombo = new ComboBox<>(FXCollections.observableArrayList(ObjectiveType.values()));
        objectiveCombo.setValue(ObjectiveType.MINIMIZE);
        objectiveCombo.setId("optObjective");

        targetVarCombo = new ComboBox<>(FXCollections.observableArrayList(stockNames));
        targetVarCombo.setId("optTargetVar");
        if (!stockNames.isEmpty()) {
            targetVarCombo.setValue(stockNames.getFirst());
        }

        targetValueLabel = new Label("Target Value:");
        targetValueField = new TextField("0");
        targetValueField.setId("optTargetValue");
        targetValueLabel.setVisible(false);
        targetValueLabel.setManaged(false);
        targetValueField.setVisible(false);
        targetValueField.setManaged(false);

        objectiveCombo.valueProperty().addListener((obs, old, val) -> {
            boolean showTarget = val == ObjectiveType.TARGET;
            targetValueLabel.setVisible(showTarget);
            targetValueLabel.setManaged(showTarget);
            targetValueField.setVisible(showTarget);
            targetValueField.setManaged(showTarget);
        });

        algorithmCombo = new ComboBox<>(FXCollections.observableArrayList(
                "NELDER_MEAD", "BOBYQA", "CMAES"));
        algorithmCombo.setValue("NELDER_MEAD");
        algorithmCombo.setId("optAlgorithm");

        maxEvalsField = new TextField("1000");
        maxEvalsField.setId("optMaxEvals");

        GridPane settingsGrid = new GridPane();
        settingsGrid.setHgap(10);
        settingsGrid.setVgap(10);
        settingsGrid.setPadding(new Insets(10, 0, 0, 0));

        settingsGrid.add(new Label("Objective:"), 0, 0);
        settingsGrid.add(objectiveCombo, 1, 0);
        settingsGrid.add(new Label("Target Variable:"), 0, 1);
        settingsGrid.add(targetVarCombo, 1, 1);
        settingsGrid.add(targetValueLabel, 0, 2);
        settingsGrid.add(targetValueField, 1, 2);
        settingsGrid.add(new Label("Algorithm:"), 0, 3);
        settingsGrid.add(algorithmCombo, 1, 3);
        settingsGrid.add(new Label("Max Evaluations:"), 0, 4);
        settingsGrid.add(maxEvalsField, 1, 4);

        return settingsGrid;
    }

    private String getValidationMessage() {
        if (targetVarCombo.getValue() == null) {
            return "Select a target variable.";
        }
        if (paramRows.stream().noneMatch(ParamRow::isValid)) {
            return "At least one parameter row must have valid bounds.";
        }
        try {
            int maxEvals = Integer.parseInt(maxEvalsField.getText().trim());
            if (maxEvals < 1) {
                return "Max evaluations must be at least 1.";
            }
        } catch (NumberFormatException e) {
            return "Max evaluations must be a valid integer.";
        }
        if (objectiveCombo.getValue() == ObjectiveType.TARGET) {
            try {
                double val = Double.parseDouble(targetValueField.getText().trim());
                if (!Double.isFinite(val)) {
                    return "Target value must be a finite number.";
                }
            } catch (NumberFormatException e) {
                return "Target value must be a valid number.";
            }
        }
        return "";
    }

    private class ParamRow extends ParameterRowBase {
        private final TextField lowerField;
        private final TextField upperField;
        private final TextField guessField;

        ParamRow(List<String> constantNames, VBox container) {
            super(constantNames, null,
                    () -> fieldChangeCounter.set(fieldChangeCounter.get() + 1));
            int rowIndex = paramRows.size();
            nameCombo.setId("optParamName" + rowIndex);

            lowerField = new TextField("0");
            lowerField.setPrefWidth(60);
            lowerField.setPromptText("Lower");
            lowerField.setId("optLower" + rowIndex);
            upperField = new TextField("10");
            upperField.setPrefWidth(60);
            upperField.setPromptText("Upper");
            upperField.setId("optUpper" + rowIndex);
            guessField = new TextField("");
            guessField.setPrefWidth(60);
            guessField.setPromptText("Guess");
            guessField.setId("optGuess" + rowIndex);

            wireFieldChange(lowerField);
            wireFieldChange(upperField);
            wireFieldChange(guessField);

            Button removeBtn = createRemoveButton(() -> {
                paramRows.remove(this);
                container.getChildren().remove(getPane());
            });

            buildPane(removeBtn,
                    new Label("Low:"), lowerField,
                    new Label("High:"), upperField,
                    new Label("Guess:"), guessField);
        }

        @Override
        public boolean isValid() {
            if (!isNameSelected()) {
                return false;
            }
            try {
                double low = Double.parseDouble(lowerField.getText().trim());
                double high = Double.parseDouble(upperField.getText().trim());
                if (low >= high) {
                    return false;
                }
                String guessText = guessField.getText().trim();
                if (!guessText.isEmpty()) {
                    double guess = Double.parseDouble(guessText);
                    return guess >= low && guess <= high;
                }
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }

        ParamConfig toConfig() {
            double low = Double.parseDouble(lowerField.getText().trim());
            double high = Double.parseDouble(upperField.getText().trim());
            String guessText = guessField.getText().trim();
            double guess = guessText.isEmpty() ? (low + high) / 2.0 : Double.parseDouble(guessText);
            return new ParamConfig(getSelectedName(), low, high, guess);
        }
    }
}

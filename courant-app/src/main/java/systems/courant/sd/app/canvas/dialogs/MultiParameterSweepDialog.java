package systems.courant.sd.app.canvas.dialogs;

import systems.courant.sd.sweep.ParameterSweep;

import javafx.beans.binding.Bindings;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import systems.courant.sd.app.canvas.HelpContextResolver;
import systems.courant.sd.app.canvas.ParameterRowBase;
import systems.courant.sd.app.canvas.Styles;

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
    private final IntegerProperty fieldChangeCounter = new SimpleIntegerProperty(0);
    private final Label combinationCountLabel;

    public MultiParameterSweepDialog(List<String> constantNames) {
        HelpContextResolver.addHelpButton(this);
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

        Label validationLabel = new Label();
        validationLabel.setStyle(Styles.VALIDATION_ERROR);
        validationLabel.setWrapText(true);
        validationLabel.setMaxWidth(Double.MAX_VALUE);
        validationLabel.setId("multiSweepValidationLabel");
        validationLabel.textProperty().bind(
                Bindings.createStringBinding(this::getValidationMessage,
                        parameterRows, fieldChangeCounter)
        );

        VBox content = new VBox(10, paramsLabel, paramScroll, validationLabel);
        content.setPadding(new Insets(10));
        getDialogPane().setContent(content);
        getDialogPane().setPrefWidth(Styles.screenAwareWidth(Styles.CONFIG_DIALOG_WIDTH));

        ButtonType okButton = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(okButton, ButtonType.CANCEL);

        getDialogPane().lookupButton(okButton).disableProperty().bind(
                Bindings.createBooleanBinding(() -> !getValidationMessage().isEmpty(),
                        parameterRows, fieldChangeCounter)
        );

        setResultConverter(button -> {
            if (button == okButton) {
                return new Config(getValidParams());
            }
            return null;
        });
    }

    private String getValidationMessage() {
        List<ParamConfig> validParams = getValidParams();
        if (validParams.size() < 2) {
            int valid = validParams.size();
            return "At least 2 valid parameter rows required (currently " + valid + ").";
        }
        Set<String> names = new HashSet<>();
        for (ParamConfig p : validParams) {
            if (!names.add(p.name())) {
                return "Duplicate parameter: " + p.name() + ". Each must be unique.";
            }
        }
        long combos = computeCombinations(validParams);
        if (combos > MAX_COMBINATIONS) {
            return "Too many combinations (" + combos + "). Maximum is " + MAX_COMBINATIONS + ".";
        }
        return "";
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
        fieldChangeCounter.set(fieldChangeCounter.get() + 1);
        List<ParamConfig> validParams = getValidParams();
        long combos = computeCombinations(validParams);
        combinationCountLabel.setText(combos + " combinations");
        if (combos > MAX_COMBINATIONS) {
            combinationCountLabel.setStyle(Styles.VALIDATION_ERROR + " -fx-font-weight: bold;");
        } else if (combos > WARN_COMBINATIONS) {
            combinationCountLabel.setStyle(Styles.VALIDATION_WARNING);
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
            try {
                product = Math.multiplyExact(product, count);
            } catch (ArithmeticException e) {
                return Long.MAX_VALUE;
            }
            if (product > 10_000_000) {
                return product;
            }
        }
        return product;
    }

    private class ParameterRow extends ParameterRowBase {
        private final TextField startField;
        private final TextField endField;
        private final TextField stepField;

        ParameterRow(List<String> constantNames, VBox container, String defaultName) {
            super(constantNames, defaultName, MultiParameterSweepDialog.this::updateCombinationCount);
            int rowIndex = parameterRows.size();
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

            wireFieldChange(startField);
            wireFieldChange(endField);
            wireFieldChange(stepField);

            Button removeBtn = createRemoveButton(() -> {
                parameterRows.remove(this);
                container.getChildren().remove(getPane());
                updateCombinationCount();
            });

            buildPane(removeBtn,
                    new Label("Start:"), startField,
                    new Label("End:"), endField,
                    new Label("Step:"), stepField);
        }

        @Override
        public boolean isValid() {
            if (!isNameSelected()) {
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
                    getSelectedName(),
                    Double.parseDouble(startField.getText().trim()),
                    Double.parseDouble(endField.getText().trim()),
                    Double.parseDouble(stepField.getText().trim()));
        }
    }
}

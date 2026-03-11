package systems.courant.shrewd.app.canvas;

import javafx.beans.binding.Bindings;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
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
 * Dialog for configuring an optimization run: parameters with bounds,
 * objective type, target variable, algorithm, and max evaluations.
 */
public class OptimizerDialog extends Dialog<OptimizerDialog.Config> {

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
    private final ComboBox<ObjectiveType> objectiveCombo;
    private final ComboBox<String> targetVarCombo;
    private final TextField targetValueField;
    private final Label targetValueLabel;
    private final ComboBox<String> algorithmCombo;
    private final TextField maxEvalsField;

    public OptimizerDialog(List<String> constantNames, List<String> stockNames) {
        setTitle("Optimize");
        setHeaderText("Configure optimization");

        VBox paramBox = new VBox(6);
        paramBox.setPadding(new Insets(5));

        Button addButton = new Button("Add Parameter");
        addButton.setOnAction(e -> {
            ParamRow row = new ParamRow(constantNames, paramBox);
            paramRows.add(row);
            paramBox.getChildren().add(paramBox.getChildren().size() - 1, row.getPane());
        });

        paramBox.getChildren().add(addButton);

        // Add one default row
        ParamRow defaultRow = new ParamRow(constantNames, paramBox);
        paramRows.add(defaultRow);
        paramBox.getChildren().add(0, defaultRow.getPane());

        ScrollPane paramScroll = new ScrollPane(paramBox);
        paramScroll.setFitToWidth(true);
        paramScroll.setPrefHeight(150);

        objectiveCombo = new ComboBox<>(FXCollections.observableArrayList(ObjectiveType.values()));
        objectiveCombo.setValue(ObjectiveType.MINIMIZE);

        targetVarCombo = new ComboBox<>(FXCollections.observableArrayList(stockNames));
        if (!stockNames.isEmpty()) {
            targetVarCombo.setValue(stockNames.getFirst());
        }

        targetValueLabel = new Label("Target Value:");
        targetValueField = new TextField("0");
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

        maxEvalsField = new TextField("1000");

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

        Label paramsLabel = new Label("Parameters");
        paramsLabel.setStyle(Styles.SECTION_HEADER);

        Label validationLabel = new Label();
        validationLabel.setStyle(Styles.VALIDATION_ERROR);
        validationLabel.setWrapText(true);
        validationLabel.setMaxWidth(Double.MAX_VALUE);
        validationLabel.setId("optimizerValidationLabel");
        validationLabel.textProperty().bind(
                Bindings.createStringBinding(this::getValidationMessage,
                        maxEvalsField.textProperty(), targetValueField.textProperty(),
                        objectiveCombo.valueProperty(), targetVarCombo.valueProperty(),
                        paramRows, fieldChangeCounter)
        );

        VBox content = new VBox(10, paramsLabel, paramScroll, settingsGrid, validationLabel);
        content.setPadding(new Insets(10));
        getDialogPane().setContent(content);
        getDialogPane().setPrefWidth(550);

        ButtonType okButton = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(okButton, ButtonType.CANCEL);

        getDialogPane().lookupButton(okButton).disableProperty().bind(
                Bindings.createBooleanBinding(this::isInvalid,
                        maxEvalsField.textProperty(), targetValueField.textProperty(),
                        objectiveCombo.valueProperty(), targetVarCombo.valueProperty(),
                        paramRows, fieldChangeCounter)
        );

        Button okNode = (Button) getDialogPane().lookupButton(okButton);
        okNode.addEventFilter(ActionEvent.ACTION, event -> {
            boolean hasValid = paramRows.stream().anyMatch(ParamRow::isValid);
            if (!hasValid) {
                event.consume();
                new Alert(Alert.AlertType.WARNING,
                        "At least one parameter row must have valid values.").showAndWait();
            }
        });

        setResultConverter(button -> {
            if (button == okButton) {
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
            return null;
        });
    }

    private boolean isInvalid() {
        return !getValidationMessage().isEmpty();
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

    private class ParamRow {
        private final ComboBox<String> nameCombo;
        private final TextField lowerField;
        private final TextField upperField;
        private final TextField guessField;
        private final HBox pane;

        ParamRow(List<String> constantNames, VBox container) {
            nameCombo = new ComboBox<>(FXCollections.observableArrayList(constantNames));
            if (!constantNames.isEmpty()) {
                nameCombo.setValue(constantNames.getFirst());
            }
            nameCombo.setPrefWidth(130);

            lowerField = new TextField("0");
            lowerField.setPrefWidth(60);
            lowerField.setPromptText("Lower");
            upperField = new TextField("10");
            upperField.setPrefWidth(60);
            upperField.setPromptText("Upper");
            guessField = new TextField("");
            guessField.setPrefWidth(60);
            guessField.setPromptText("Guess");

            lowerField.textProperty().addListener((obs, o, n) ->
                    fieldChangeCounter.set(fieldChangeCounter.get() + 1));
            upperField.textProperty().addListener((obs, o, n) ->
                    fieldChangeCounter.set(fieldChangeCounter.get() + 1));
            guessField.textProperty().addListener((obs, o, n) ->
                    fieldChangeCounter.set(fieldChangeCounter.get() + 1));

            pane = new HBox(6);
            pane.setPadding(new Insets(2));

            Button removeBtn = new Button("X");
            removeBtn.setOnAction(e -> {
                paramRows.remove(this);
                container.getChildren().remove(pane);
            });

            pane.getChildren().addAll(
                    nameCombo,
                    new Label("Low:"), lowerField,
                    new Label("High:"), upperField,
                    new Label("Guess:"), guessField,
                    removeBtn);
        }

        HBox getPane() {
            return pane;
        }

        boolean isValid() {
            if (nameCombo.getValue() == null) {
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
            double guess = guessText.isEmpty() ? Double.NaN : Double.parseDouble(guessText);
            return new ParamConfig(nameCombo.getValue(), low, high, guess);
        }
    }
}

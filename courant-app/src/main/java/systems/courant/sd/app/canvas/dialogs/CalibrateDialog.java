package systems.courant.sd.app.canvas.dialogs;

import javafx.beans.binding.Bindings;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
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
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import systems.courant.sd.app.canvas.HelpContextResolver;
import systems.courant.sd.app.canvas.ParameterRowBase;
import systems.courant.sd.app.canvas.Styles;
import systems.courant.sd.io.ReferenceDataCsvReader;
import systems.courant.sd.model.def.ReferenceDataset;

/**
 * Dialog for configuring a calibration run: import observed CSV data,
 * map columns to model stocks, set parameter bounds, and choose algorithm.
 */
public class CalibrateDialog extends Dialog<CalibrateDialog.Config> {

    public record FitTarget(String stockName, String csvColumnName, double[] observedData) {
    }

    public record ParamConfig(String name, double lower, double upper, double initialGuess) {
    }

    public record Config(
            List<FitTarget> fitTargets,
            List<ParamConfig> parameters,
            String algorithm,
            int maxEvaluations
    ) {
    }

    private final ObservableList<ParamRow> paramRows = FXCollections.observableArrayList();
    private final IntegerProperty fieldChangeCounter = new SimpleIntegerProperty(0);
    private Label datasetLabel;
    private ComboBox<String> algorithmCombo;
    private TextField maxEvalsField;
    private Label validationLabel;
    private VBox fitTargetBox;
    private final List<String> stockNames;

    private ReferenceDataset importedDataset;
    private final List<FitTargetRow> fitTargetRows = new ArrayList<>();

    public CalibrateDialog(List<String> constantNames, List<String> stockNames) {
        this.stockNames = stockNames;
        HelpContextResolver.addHelpButton(this);
        setTitle("Calibrate");
        setHeaderText("Fit model parameters to observed data");

        VBox content = new VBox(10,
                buildObservedDataSection(),
                buildFitTargetsSection(),
                buildParametersSection(constantNames),
                buildSettingsSection(),
                buildValidationLabel());
        content.setPadding(new Insets(10));
        getDialogPane().setContent(content);
        getDialogPane().setPrefWidth(Styles.screenAwareWidth(Styles.CONFIG_DIALOG_WIDTH));

        configureButtons();
    }

    private VBox buildObservedDataSection() {
        Label dataLabel = new Label("Observed Data");
        dataLabel.setStyle(Styles.SECTION_HEADER);

        datasetLabel = new Label("No data loaded");
        datasetLabel.setId("calibDatasetLabel");

        Button importBtn = new Button("Import CSV...");
        importBtn.setId("calibImportCsv");
        importBtn.setOnAction(e -> importCsv());

        HBox dataRow = new HBox(10, importBtn, datasetLabel);
        return new VBox(10, dataLabel, dataRow);
    }

    private VBox buildFitTargetsSection() {
        Label fitLabel = new Label("Fit Targets");
        fitLabel.setStyle(Styles.SECTION_HEADER);

        fitTargetBox = new VBox(6);
        fitTargetBox.setPadding(new Insets(5));

        return new VBox(10, fitLabel, fitTargetBox);
    }

    private VBox buildParametersSection(List<String> constantNames) {
        Label paramsLabel = new Label("Parameters");
        paramsLabel.setStyle(Styles.SECTION_HEADER);

        VBox paramBox = new VBox(6);
        paramBox.setPadding(new Insets(5));

        Button addButton = new Button("Add Parameter");
        addButton.setId("calibAddParam");
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

        return new VBox(10, paramsLabel, paramScroll);
    }

    private GridPane buildSettingsSection() {
        Label settingsLabel = new Label("Settings");
        settingsLabel.setStyle(Styles.SECTION_HEADER);

        algorithmCombo = new ComboBox<>(FXCollections.observableArrayList(
                "NELDER_MEAD", "BOBYQA", "CMAES"));
        algorithmCombo.setValue("NELDER_MEAD");
        algorithmCombo.setId("calibAlgorithm");

        maxEvalsField = new TextField("1000");
        maxEvalsField.setId("calibMaxEvals");

        GridPane settingsGrid = new GridPane();
        settingsGrid.setHgap(10);
        settingsGrid.setVgap(10);
        settingsGrid.setPadding(new Insets(10, 0, 0, 0));

        settingsGrid.add(new Label("Algorithm:"), 0, 0);
        settingsGrid.add(algorithmCombo, 1, 0);
        settingsGrid.add(new Label("Max Evaluations:"), 0, 1);
        settingsGrid.add(maxEvalsField, 1, 1);

        return settingsGrid;
    }

    private Label buildValidationLabel() {
        validationLabel = new Label();
        validationLabel.setStyle(Styles.VALIDATION_ERROR);
        validationLabel.setWrapText(true);
        validationLabel.setMaxWidth(Double.MAX_VALUE);
        validationLabel.setId("calibValidationLabel");
        validationLabel.textProperty().bind(
                Bindings.createStringBinding(this::getValidationMessage,
                        maxEvalsField.textProperty(), paramRows, fieldChangeCounter)
        );
        return validationLabel;
    }

    private void configureButtons() {
        ButtonType okButton = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(okButton, ButtonType.CANCEL);

        getDialogPane().lookupButton(okButton).disableProperty().bind(
                Bindings.createBooleanBinding(this::isInvalid,
                        maxEvalsField.textProperty(), paramRows, fieldChangeCounter)
        );

        setResultConverter(button -> {
            if (button == okButton) {
                return buildConfig();
            }
            return null;
        });
    }

    private Config buildConfig() {
        List<FitTarget> targets = new ArrayList<>();
        for (FitTargetRow row : fitTargetRows) {
            String stock = row.stockCombo.getValue();
            if (stock != null && !stock.isEmpty()) {
                double[] data = importedDataset.columns().get(row.csvColumn);
                if (data == null) {
                    continue;
                }
                targets.add(new FitTarget(stock, row.csvColumn, data));
            }
        }
        List<ParamConfig> params = new ArrayList<>();
        for (ParamRow row : paramRows) {
            if (row.isValid()) {
                params.add(row.toConfig());
            }
        }
        return new Config(
                targets,
                params,
                algorithmCombo.getValue(),
                Integer.parseInt(maxEvalsField.getText().trim())
        );
    }

    private void importCsv() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Import Observed Data CSV");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File file = chooser.showOpenDialog(getDialogPane().getScene().getWindow());
        if (file == null) {
            return;
        }
        try {
            importedDataset = ReferenceDataCsvReader.read(file.toPath(), file.getName());
            datasetLabel.setText(file.getName());

            // Show column mapping dialog
            ColumnMappingDialog mappingDialog = new ColumnMappingDialog(importedDataset, stockNames);
            var mapped = mappingDialog.showAndWait().orElse(null);
            if (mapped != null) {
                importedDataset = mapped;
                buildFitTargetRows();
            } else {
                importedDataset = null;
                fitTargetRows.clear();
                fitTargetBox.getChildren().clear();
                datasetLabel.setText("No columns mapped — import again");
            }
        } catch (IOException ex) {
            datasetLabel.setText("Error: " + ex.getMessage());
        }
        fieldChangeCounter.set(fieldChangeCounter.get() + 1);
    }

    private void buildFitTargetRows() {
        fitTargetRows.clear();
        fitTargetBox.getChildren().clear();

        for (String csvCol : importedDataset.variableNames()) {
            FitTargetRow row = new FitTargetRow(csvCol);
            fitTargetRows.add(row);
            fitTargetBox.getChildren().add(row.pane);
        }
        fieldChangeCounter.set(fieldChangeCounter.get() + 1);
    }

    private boolean isInvalid() {
        return !getValidationMessage().isEmpty();
    }

    private String getValidationMessage() {
        if (importedDataset == null) {
            return "Import CSV observed data first.";
        }
        if (fitTargetRows.isEmpty()
                || fitTargetRows.stream().noneMatch(r -> r.stockCombo.getValue() != null
                && !r.stockCombo.getValue().isEmpty())) {
            return "At least one fit target must be mapped.";
        }
        if (paramRows.stream().noneMatch(ParamRow::isValid)) {
            return "Add at least one parameter with valid bounds.";
        }
        try {
            int maxEvals = Integer.parseInt(maxEvalsField.getText().trim());
            if (maxEvals < 1) {
                return "Max evaluations must be at least 1.";
            }
        } catch (NumberFormatException e) {
            return "Max evaluations must be a valid integer.";
        }
        return "";
    }

    private class FitTargetRow {
        final String csvColumn;
        final ComboBox<String> stockCombo;
        final HBox pane;

        FitTargetRow(String csvColumn) {
            this.csvColumn = csvColumn;

            stockCombo = new ComboBox<>(FXCollections.observableArrayList(stockNames));
            stockCombo.setPrefWidth(150);

            // Auto-select matching stock
            stockNames.stream()
                    .filter(s -> s.equalsIgnoreCase(csvColumn)
                            || s.replace(" ", "_").equalsIgnoreCase(csvColumn)
                            || s.equalsIgnoreCase(csvColumn.replace("_", " ")))
                    .findFirst()
                    .ifPresent(stockCombo::setValue);

            stockCombo.valueProperty().addListener(
                    (obs, o, n) -> fieldChangeCounter.set(fieldChangeCounter.get() + 1));

            pane = new HBox(10,
                    new Label(csvColumn + " \u2192"),
                    stockCombo);
            pane.setPadding(new Insets(2));
        }
    }

    private class ParamRow extends ParameterRowBase {
        private final TextField lowerField;
        private final TextField upperField;
        private final TextField guessField;

        ParamRow(List<String> constantNames, VBox container) {
            super(constantNames, null,
                    () -> fieldChangeCounter.set(fieldChangeCounter.get() + 1));
            int rowIndex = paramRows.size();
            nameCombo.setId("calibParamName" + rowIndex);

            lowerField = new TextField("0");
            lowerField.setPrefWidth(60);
            lowerField.setPromptText("Lower");
            lowerField.setId("calibLower" + rowIndex);
            upperField = new TextField("10");
            upperField.setPrefWidth(60);
            upperField.setPromptText("Upper");
            upperField.setId("calibUpper" + rowIndex);
            guessField = new TextField("");
            guessField.setPrefWidth(60);
            guessField.setPromptText("Guess");
            guessField.setId("calibGuess" + rowIndex);

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
            double guess = guessText.isEmpty() ? Double.NaN : Double.parseDouble(guessText);
            return new ParamConfig(getSelectedName(), low, high, guess);
        }
    }
}

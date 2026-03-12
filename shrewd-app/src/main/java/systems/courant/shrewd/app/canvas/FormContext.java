package systems.courant.shrewd.app.canvas;

import systems.courant.shrewd.measure.CompositeUnit;
import systems.courant.shrewd.measure.DimensionalAnalyzer;
import systems.courant.shrewd.measure.UnitRegistry;
import systems.courant.shrewd.model.expr.Expr;
import systems.courant.shrewd.model.expr.ExprParser;
import systems.courant.shrewd.model.expr.ParseException;

import javafx.animation.PauseTransition;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.util.Duration;

import java.util.List;
import java.util.function.Consumer;

/**
 * Shared mutable context passed to {@link ElementForm} implementations.
 * Holds references to the canvas, editor, property grid, and the current element name.
 * Also provides UI helper methods for building form rows.
 */
class FormContext {

    ModelCanvas canvas;
    ModelEditor editor;
    GridPane grid;
    String elementName;
    boolean updatingFields;
    Runnable onFormRebuildRequested;
    Runnable onOpenExpressionHelp;

    /** Cached registry for dimensional analysis — avoids rebuilding on every keystroke. */
    private final UnitRegistry unitRegistry = new UnitRegistry();

    void requestFormRebuild() {
        if (onFormRebuildRequested != null) {
            onFormRebuildRequested.run();
        }
    }

    TextField createTextField(String text) {
        TextField field = new TextField(text);
        field.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(field, Priority.ALWAYS);
        return field;
    }

    /**
     * Creates a multi-line equation editor with syntax highlighting.
     */
    EquationField createEquationField(String text) {
        CodeAreaEquationField field = new CodeAreaEquationField(text);
        Node node = field.node();
        node.setStyle(node.getStyle() + "; -fx-max-width: infinity;");
        GridPane.setHgrow(node, Priority.ALWAYS);
        return field;
    }

    TextField createNameField() {
        TextField nameField = createTextField(elementName);
        nameField.setId("nameField");
        nameField.setOnAction(e -> commitRename(nameField));
        nameField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused && !updatingFields) {
                commitRename(nameField);
            }
        });
        return nameField;
    }

    /**
     * Wraps an equation field in an HBox with a "?" help button that opens
     * the Expression Language dialog and a template button.
     */
    Node wrapWithHelpButton(EquationField equationField) {
        if (onOpenExpressionHelp == null) {
            return equationField.node();
        }
        Button helpBtn = new Button("?");
        helpBtn.setId("equationHelpButton");
        helpBtn.setMinWidth(24);
        helpBtn.setMaxWidth(24);
        helpBtn.setMinHeight(24);
        helpBtn.setMaxHeight(24);
        helpBtn.setFocusTraversable(false);
        helpBtn.setStyle("-fx-font-size: 11; -fx-padding: 0;");
        Tooltip.install(helpBtn, new Tooltip("Open function reference"));
        helpBtn.setOnAction(e -> onOpenExpressionHelp.run());

        Button templateBtn = new Button("\u2261");
        templateBtn.setId("equationTemplateButton");
        templateBtn.setMinWidth(24);
        templateBtn.setMaxWidth(24);
        templateBtn.setMinHeight(24);
        templateBtn.setMaxHeight(24);
        templateBtn.setFocusTraversable(false);
        templateBtn.setStyle("-fx-font-size: 11; -fx-padding: 0;");
        Tooltip.install(templateBtn, new Tooltip("Insert equation template"));
        ContextMenu templateMenu = EquationTemplates.createMenu(equationField);
        templateBtn.setOnAction(e ->
                templateMenu.show(templateBtn, Side.BOTTOM, 0, 0));

        Node fieldNode = equationField.node();
        HBox box = new HBox(4, fieldNode, templateBtn, helpBtn);
        box.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(fieldNode, Priority.ALWAYS);
        GridPane.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    void addFieldRow(int row, String labelText, Node field) {
        addFieldRow(row, labelText, field, null);
    }

    void addFieldRow(int row, String labelText, Node field, String helpText) {
        Label label = new Label(labelText);
        label.setStyle(Styles.FIELD_LABEL);
        grid.add(buildLabelNode(label, helpText), 0, row);
        grid.add(field, 1, row);
    }

    void addReadOnlyRow(int row, String labelText, String value) {
        addReadOnlyRow(row, labelText, value, null);
    }

    void addReadOnlyRow(int row, String labelText, String value, String helpText) {
        Label label = new Label(labelText);
        label.setStyle(Styles.FIELD_LABEL);
        Label valueLabel = new Label(value);
        valueLabel.setStyle(Styles.SMALL_TEXT);
        valueLabel.setWrapText(true);
        grid.add(buildLabelNode(label, helpText), 0, row);
        grid.add(valueLabel, 1, row);
    }

    void addReadOnlyRow(int row, String labelText, Label valueLabel) {
        addReadOnlyRow(row, labelText, valueLabel, null);
    }

    void addReadOnlyRow(int row, String labelText, Label valueLabel, String helpText) {
        Label label = new Label(labelText);
        label.setStyle(Styles.FIELD_LABEL);
        valueLabel.setStyle(Styles.SMALL_TEXT);
        valueLabel.setWrapText(true);
        grid.add(buildLabelNode(label, helpText), 0, row);
        grid.add(valueLabel, 1, row);
    }

    private Node buildLabelNode(Label label, String helpText) {
        if (helpText == null) {
            return label;
        }
        Label icon = new Label("\u24D8");
        icon.setStyle(Styles.HELP_ICON);
        Tooltip tip = new Tooltip(helpText);
        tip.setWrapText(true);
        tip.setMaxWidth(360);
        tip.setStyle("-fx-font-size: 13px;");
        tip.setShowDelay(Duration.millis(300));
        Tooltip.install(icon, tip);
        HBox box = new HBox(4, label, icon);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    void addCommitHandlers(TextField field, Consumer<TextField> handler) {
        field.setOnAction(e -> handler.accept(field));
        field.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused && !updatingFields) {
                handler.accept(field);
            }
        });
    }

    /**
     * Adds commit handlers to an {@link EquationField}. Enter commits; focus loss commits.
     */
    void addEquationCommitHandlers(EquationField field, Consumer<EquationField> handler) {
        field.setOnAction(e -> handler.accept(field));
        field.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused && !updatingFields) {
                handler.accept(field);
            }
        });
    }

    /** Common unit names for stock/aux/constant unit fields. */
    static final List<String> COMMON_UNITS = List.of(
            "Dimensionless",
            "Person", "Thing",
            "Day", "Week", "Month", "Year",
            "Hour", "Minute", "Second",
            "USD",
            "Kilogram", "Gram", "Pound",
            "Meter", "Kilometer", "Mile",
            "Liter", "Gallon US",
            "Celsius", "Fahrenheit"
    );

    /** Time unit names for flow time unit fields. */
    static final List<String> TIME_UNITS = List.of(
            "Day", "Week", "Month", "Year",
            "Hour", "Minute", "Second", "Millisecond"
    );

    ComboBox<String> createUnitComboBox(String currentValue) {
        ComboBox<String> box = new ComboBox<>();
        box.getItems().addAll(COMMON_UNITS);
        box.setEditable(true);
        box.setValue(currentValue != null ? currentValue : "");
        box.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    ComboBox<String> createTimeUnitComboBox(String currentValue) {
        ComboBox<String> box = new ComboBox<>();
        box.getItems().addAll(TIME_UNITS);
        box.setEditable(true);
        box.setValue(currentValue != null ? currentValue : "");
        box.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    void addTextAreaCommitHandlers(TextArea area, Consumer<TextArea> handler) {
        area.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused && !updatingFields) {
                handler.accept(area);
            }
        });
    }

    void addComboCommitHandlers(ComboBox<String> box, Consumer<ComboBox<String>> handler) {
        box.setOnAction(e -> {
            if (!updatingFields) {
                handler.accept(box);
            }
        });
        box.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused && !updatingFields) {
                handler.accept(box);
            }
        });
    }

    /**
     * Attaches real-time equation validation and dimensional analysis to an equation field.
     * Shows a red border and error label for syntax/reference errors, and an inferred
     * unit label for dimensional analysis feedback.
     *
     * @param field   the equation field
     * @param row     the grid row where the equation field sits
     * @return the error label (for cleanup if needed)
     */
    Label attachEquationValidation(EquationField field, int row) {
        Label errorLabel = new Label();
        errorLabel.setStyle(Styles.EQUATION_ERROR_LABEL);
        errorLabel.setWrapText(true);
        errorLabel.setMaxWidth(Double.MAX_VALUE);
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        GridPane.setHgrow(errorLabel, Priority.ALWAYS);
        grid.add(errorLabel, 1, row);

        Label dimensionLabel = new Label();
        dimensionLabel.setStyle(Styles.DIMENSION_LABEL);
        dimensionLabel.setWrapText(true);
        dimensionLabel.setMaxWidth(Double.MAX_VALUE);
        dimensionLabel.setVisible(false);
        dimensionLabel.setManaged(false);
        GridPane.setHgrow(dimensionLabel, Priority.ALWAYS);
        // Dimension label goes in same row — we'll toggle visibility with error label
        grid.add(dimensionLabel, 1, row);

        PauseTransition debounce = new PauseTransition(Duration.millis(400));
        debounce.setOnFinished(e -> validateEquation(field, errorLabel, dimensionLabel));

        field.textObservable().addListener((obs, oldVal, newVal) -> {
            if (!updatingFields) {
                debounce.playFromStart();
            }
        });

        field.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused && !updatingFields) {
                debounce.stop();
                validateEquation(field, errorLabel, dimensionLabel);
            }
        });

        // Initial validation
        validateEquation(field, errorLabel, dimensionLabel);

        return errorLabel;
    }

    private void validateEquation(EquationField field, Label errorLabel, Label dimensionLabel) {
        String text = field.getText().trim();
        if (text.isEmpty()) {
            clearEquationError(field, errorLabel);
            hideDimensionLabel(dimensionLabel);
            return;
        }
        EquationValidator.Result result =
                EquationValidator.validate(text, editor, elementName);
        if (result.valid()) {
            clearEquationError(field, errorLabel);
            runDimensionalAnalysis(text, dimensionLabel);
        } else {
            field.setFieldStyle(Styles.EQUATION_ERROR_BORDER);
            errorLabel.setText(result.message());
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
            hideDimensionLabel(dimensionLabel);
        }
    }

    private void runDimensionalAnalysis(String equationText, Label dimensionLabel) {
        try {
            Expr expr = ExprParser.parse(equationText);
            EditorUnitContext unitContext = new EditorUnitContext(editor, unitRegistry);
            DimensionalAnalyzer analyzer = new DimensionalAnalyzer(unitContext);
            DimensionalAnalyzer.AnalysisResult analysis = analyzer.analyze(expr);

            if (analysis.inferredUnit() == null) {
                hideDimensionLabel(dimensionLabel);
                return;
            }

            // Build display text
            String inferredDisplay = analysis.inferredUnit().displayString();
            CompositeUnit expected = getExpectedUnit(unitRegistry);

            if (!analysis.isConsistent()) {
                // Show first warning
                String warning = analysis.warnings().getFirst().message();
                dimensionLabel.setText("Warning: " + warning);
                dimensionLabel.setStyle(Styles.DIMENSION_MISMATCH);
            } else if (expected != null && !expected.isCompatibleWith(analysis.inferredUnit())) {
                dimensionLabel.setText("Equation yields " + inferredDisplay
                        + ", expected " + expected.displayString());
                dimensionLabel.setStyle(Styles.DIMENSION_MISMATCH);
            } else {
                dimensionLabel.setText("= " + inferredDisplay);
                dimensionLabel.setStyle(expected != null ? Styles.DIMENSION_MATCH
                        : Styles.DIMENSION_LABEL);
            }
            dimensionLabel.setVisible(true);
            dimensionLabel.setManaged(true);
        } catch (ParseException e) {
            hideDimensionLabel(dimensionLabel);
        }
    }

    /**
     * Returns the expected composite unit for the current element, or null if unknown.
     */
    private CompositeUnit getExpectedUnit(UnitRegistry registry) {
        // For flows: expected is material / time
        var flowOpt = editor.getFlowByName(elementName);
        if (flowOpt.isPresent()) {
            var flow = flowOpt.get();
            systems.courant.shrewd.measure.Unit materialUnit = null;
            if (flow.materialUnit() != null && !flow.materialUnit().isBlank()) {
                materialUnit = registry.resolve(flow.materialUnit());
            } else if (flow.sink() != null) {
                var sink = editor.getStockByName(flow.sink());
                if (sink.isPresent() && sink.get().unit() != null
                        && !sink.get().unit().isBlank()) {
                    materialUnit = registry.resolve(sink.get().unit());
                }
            } else if (flow.source() != null) {
                var source = editor.getStockByName(flow.source());
                if (source.isPresent() && source.get().unit() != null
                        && !source.get().unit().isBlank()) {
                    materialUnit = registry.resolve(source.get().unit());
                }
            }
            try {
                var timeUnit = registry.resolveTimeUnit(flow.timeUnit());
                return CompositeUnit.ofRate(materialUnit, timeUnit);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }

        // For variables: expected is the declared unit
        var auxOpt = editor.getVariableByName(elementName);
        if (auxOpt.isPresent()) {
            String unitName = auxOpt.get().unit();
            if (unitName != null && !unitName.isBlank()) {
                return CompositeUnit.of(registry.resolve(unitName));
            }
        }

        return null;
    }

    private void clearEquationError(EquationField field, Label errorLabel) {
        field.setFieldStyle("");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    private void hideDimensionLabel(Label dimensionLabel) {
        dimensionLabel.setVisible(false);
        dimensionLabel.setManaged(false);
    }

    /**
     * Briefly highlights a text field with a red border and tooltip to indicate
     * that the entered value was invalid and has been reverted. The border fades
     * after 2 seconds.
     */
    void flashInvalidInput(TextField field) {
        String original = field.getStyle();
        field.setStyle(Styles.EQUATION_ERROR_BORDER);
        Tooltip tip = new Tooltip("Invalid number \u2014 reverted to previous value");
        tip.setShowDelay(Duration.ZERO);
        Tooltip.install(field, tip);
        PauseTransition fade = new PauseTransition(Duration.seconds(2));
        fade.setOnFinished(e -> {
            field.setStyle(original);
            Tooltip.uninstall(field, tip);
        });
        fade.play();
    }

    private void commitRename(TextField nameField) {
        String oldName = elementName;
        String newName = nameField.getText().trim();
        if (newName.isEmpty() || newName.equals(oldName) || !ModelEditor.isValidName(newName)) {
            nameField.setText(oldName);
            return;
        }
        if (editor.hasElement(newName)) {
            nameField.setText(oldName);
            return;
        }
        canvas.renameElement(oldName, newName);
        elementName = newName;
    }
}

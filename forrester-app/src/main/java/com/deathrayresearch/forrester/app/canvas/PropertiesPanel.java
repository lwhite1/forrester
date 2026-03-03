package com.deathrayresearch.forrester.app.canvas;

import com.deathrayresearch.forrester.model.def.AuxDef;
import com.deathrayresearch.forrester.model.def.ConstantDef;
import com.deathrayresearch.forrester.model.def.ElementType;
import com.deathrayresearch.forrester.model.def.FlowDef;
import com.deathrayresearch.forrester.model.def.ModuleInstanceDef;
import com.deathrayresearch.forrester.model.def.StockDef;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Right-side panel that displays properties of the currently selected canvas element.
 * Updates whenever the canvas selection changes. Contains a context toolbar with
 * action buttons and a form section with editable property fields.
 *
 * <p>Bug fixes over the original implementation:</p>
 * <ul>
 *   <li><b>Stale name:</b> All commit handlers use {@code currentElementName} (mutable field)
 *       instead of the captured parameter, so renames propagate to subsequent edits.</li>
 *   <li><b>updatingFields guard:</b> {@link #updateSelection} sets the flag so focus-loss
 *       listeners don't fire spurious commits during programmatic updates.</li>
 *   <li><b>Double commit:</b> Each commit checks if the value actually changed before
 *       pushing undo state, so Enter + focus-loss doesn't push two undo entries.</li>
 * </ul>
 */
public class PropertiesPanel extends VBox {

    private static final double PREFERRED_WIDTH = 250;

    private final HBox contextToolbar = new HBox(4);
    private final Separator separator = new Separator();
    private final ScrollPane scrollPane = new ScrollPane();
    private final GridPane propertyGrid = new GridPane();
    private final Label placeholderLabel = new Label("No selection");

    private ModelCanvas canvas;
    private ModelEditor editor;
    private boolean updatingFields;

    /** Tracks the current element name, updated after renames to fix stale-name bug. */
    private String currentElementName;

    public PropertiesPanel() {
        setPrefWidth(PREFERRED_WIDTH);
        setMinWidth(180);
        setPadding(new Insets(4));
        setSpacing(4);

        contextToolbar.setPadding(new Insets(2));
        contextToolbar.setAlignment(Pos.CENTER_LEFT);

        propertyGrid.setHgap(8);
        propertyGrid.setVgap(6);
        propertyGrid.setPadding(new Insets(8));

        scrollPane.setContent(propertyGrid);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        placeholderLabel.setStyle(Styles.PLACEHOLDER_TEXT);
        placeholderLabel.setPadding(new Insets(20));
        placeholderLabel.setAlignment(Pos.CENTER);
        placeholderLabel.setMaxWidth(Double.MAX_VALUE);

        showPlaceholder();
    }

    /**
     * Updates the panel to reflect the current selection on the canvas.
     * Called by ForresterApp whenever the canvas status changes.
     * Wrapped in updatingFields guard to prevent spurious focus-loss commits.
     */
    public void updateSelection(ModelCanvas canvas, ModelEditor editor) {
        this.canvas = canvas;
        this.editor = editor;

        updatingFields = true;
        try {
            if (canvas == null || editor == null) {
                showPlaceholder();
                return;
            }

            Set<String> selection = canvas.getSelectedElementNames();

            if (selection.isEmpty()) {
                ConnectionId conn = canvas.getSelectedConnection();
                if (conn != null) {
                    showConnectionProperties(conn);
                } else {
                    showPlaceholder();
                }
            } else if (selection.size() == 1) {
                String name = selection.iterator().next();
                ElementType type = canvas.getSelectedElementType(name);
                currentElementName = name;
                showSingleElement(name, type);
            } else {
                showMultiSelection(selection.size());
            }
        } finally {
            updatingFields = false;
        }
    }

    private void showPlaceholder() {
        currentElementName = null;
        getChildren().clear();
        getChildren().add(placeholderLabel);
    }

    private void showMultiSelection(int count) {
        currentElementName = null;
        getChildren().clear();

        contextToolbar.getChildren().clear();
        Button deleteBtn = createToolbarButton("Delete");
        deleteBtn.setOnAction(e -> {
            if (canvas != null) {
                canvas.deleteSelectedElements();
                canvas.requestFocus();
            }
        });
        contextToolbar.getChildren().add(deleteBtn);

        propertyGrid.getChildren().clear();
        addReadOnlyRow(0, "Selection", count + " elements selected");

        getChildren().addAll(contextToolbar, separator, scrollPane);
    }

    private void showConnectionProperties(ConnectionId connection) {
        currentElementName = null;
        getChildren().clear();

        contextToolbar.getChildren().clear();

        propertyGrid.getChildren().clear();
        int row = 0;
        addReadOnlyRow(row++, "Type", "Info Link");
        addReadOnlyRow(row++, "From", connection.from());
        addReadOnlyRow(row++, "To", connection.to());

        getChildren().addAll(contextToolbar, separator, scrollPane);
    }

    private void showSingleElement(String name, ElementType type) {
        getChildren().clear();

        // Build context toolbar
        contextToolbar.getChildren().clear();

        Button renameBtn = createToolbarButton("Rename");
        renameBtn.setOnAction(e -> {
            for (javafx.scene.Node node : propertyGrid.getChildren()) {
                if (node instanceof TextField tf && "nameField".equals(tf.getId())) {
                    tf.requestFocus();
                    tf.selectAll();
                    return;
                }
            }
        });

        Button deleteBtn = createToolbarButton("Delete");
        deleteBtn.setOnAction(e -> {
            if (canvas != null) {
                canvas.deleteSelectedElements();
                canvas.requestFocus();
            }
        });

        contextToolbar.getChildren().addAll(renameBtn, deleteBtn);

        if (type == ElementType.MODULE) {
            Button drillBtn = createToolbarButton("Drill Into");
            drillBtn.setOnAction(e -> {
                if (canvas != null) {
                    canvas.drillInto(currentElementName);
                    canvas.requestFocus();
                }
            });

            Button bindingsBtn = createToolbarButton("Bindings");
            bindingsBtn.setOnAction(e -> {
                if (canvas != null) {
                    canvas.triggerBindingConfig(currentElementName);
                    canvas.requestFocus();
                }
            });

            contextToolbar.getChildren().addAll(drillBtn, bindingsBtn);
        }

        // Build property form
        propertyGrid.getChildren().clear();
        int row = 0;

        addReadOnlyRow(row++, "Type", formatType(type));

        if (type == null) {
            getChildren().addAll(contextToolbar, separator, scrollPane);
            return;
        }

        switch (type) {
            case STOCK -> row = buildStockForm(row);
            case FLOW -> row = buildFlowForm(row);
            case AUX -> row = buildAuxForm(row);
            case CONSTANT -> row = buildConstantForm(row);
            case MODULE -> row = buildModuleForm(row);
            default -> addReadOnlyRow(row++, "Name", name);
        }

        getChildren().addAll(contextToolbar, separator, scrollPane);
    }

    private int buildStockForm(int row) {
        StockDef stock = editor.getStockByName(currentElementName);
        if (stock == null) {
            addReadOnlyRow(row++, "Name", currentElementName);
            return row;
        }

        TextField nameField = createNameField(currentElementName);
        addFieldRow(row++, "Name", nameField);

        TextField initialValueField = createTextField(
                ElementRenderer.formatValue(stock.initialValue()));
        addCommitHandlers(initialValueField, this::commitStockInitialValue);
        addFieldRow(row++, "Initial Value", initialValueField);

        TextField unitField = createTextField(stock.unit() != null ? stock.unit() : "");
        addCommitHandlers(unitField, this::commitStockUnit);
        addFieldRow(row++, "Unit", unitField);

        ComboBox<String> policyBox = new ComboBox<>();
        policyBox.getItems().addAll("Allow", "Clamp to Zero");
        if ("CLAMP_TO_ZERO".equals(stock.negativeValuePolicy())) {
            policyBox.setValue("Clamp to Zero");
        } else {
            policyBox.setValue("Allow");
        }
        policyBox.setMaxWidth(Double.MAX_VALUE);
        policyBox.setOnAction(e -> {
            if (!updatingFields) {
                String policyValue = "Clamp to Zero".equals(policyBox.getValue())
                        ? "CLAMP_TO_ZERO" : null;
                StockDef s = editor.getStockByName(currentElementName);
                if (s != null && Objects.equals(policyValue, s.negativeValuePolicy())) {
                    return;
                }
                canvas.applyStockNegativeValuePolicy(currentElementName, policyValue);
            }
        });
        addFieldRow(row++, "Policy", policyBox);

        return row;
    }

    private int buildFlowForm(int row) {
        FlowDef flow = editor.getFlowByName(currentElementName);
        if (flow == null) {
            addReadOnlyRow(row++, "Name", currentElementName);
            return row;
        }

        TextField nameField = createNameField(currentElementName);
        addFieldRow(row++, "Name", nameField);

        TextField equationField = createTextField(flow.equation());
        addCommitHandlers(equationField, this::commitFlowEquation);
        addFieldRow(row++, "Equation", equationField);

        TextField timeUnitField = createTextField(
                flow.timeUnit() != null ? flow.timeUnit() : "");
        addCommitHandlers(timeUnitField, this::commitFlowTimeUnit);
        addFieldRow(row++, "Time Unit", timeUnitField);

        addReadOnlyRow(row++, "Source", flow.source() != null ? flow.source() : "(cloud)");
        addReadOnlyRow(row++, "Sink", flow.sink() != null ? flow.sink() : "(cloud)");

        return row;
    }

    private int buildAuxForm(int row) {
        AuxDef aux = editor.getAuxByName(currentElementName);
        if (aux == null) {
            addReadOnlyRow(row++, "Name", currentElementName);
            return row;
        }

        TextField nameField = createNameField(currentElementName);
        addFieldRow(row++, "Name", nameField);

        TextField equationField = createTextField(aux.equation());
        addCommitHandlers(equationField, this::commitAuxEquation);
        addFieldRow(row++, "Equation", equationField);

        TextField unitField = createTextField(aux.unit() != null ? aux.unit() : "");
        addCommitHandlers(unitField, this::commitAuxUnit);
        addFieldRow(row++, "Unit", unitField);

        return row;
    }

    private int buildConstantForm(int row) {
        ConstantDef constant = editor.getConstantByName(currentElementName);
        if (constant == null) {
            addReadOnlyRow(row++, "Name", currentElementName);
            return row;
        }

        TextField nameField = createNameField(currentElementName);
        addFieldRow(row++, "Name", nameField);

        TextField valueField = createTextField(
                ElementRenderer.formatValue(constant.value()));
        addCommitHandlers(valueField, this::commitConstantValue);
        addFieldRow(row++, "Value", valueField);

        TextField unitField = createTextField(
                constant.unit() != null ? constant.unit() : "");
        addCommitHandlers(unitField, this::commitConstantUnit);
        addFieldRow(row++, "Unit", unitField);

        return row;
    }

    private int buildModuleForm(int row) {
        ModuleInstanceDef module = editor.getModuleByName(currentElementName);
        if (module == null) {
            addReadOnlyRow(row++, "Name", currentElementName);
            return row;
        }

        TextField nameField = createNameField(currentElementName);
        addFieldRow(row++, "Instance Name", nameField);

        Map<String, String> inputs = module.inputBindings();
        if (inputs.isEmpty()) {
            addReadOnlyRow(row++, "Inputs", "(none)");
        } else {
            boolean first = true;
            for (Map.Entry<String, String> entry : inputs.entrySet()) {
                addReadOnlyRow(row++, first ? "Inputs" : "",
                        entry.getKey() + " = " + entry.getValue());
                first = false;
            }
        }

        Map<String, String> outputs = module.outputBindings();
        if (outputs.isEmpty()) {
            addReadOnlyRow(row++, "Outputs", "(none)");
        } else {
            boolean first = true;
            for (Map.Entry<String, String> entry : outputs.entrySet()) {
                addReadOnlyRow(row++, first ? "Outputs" : "",
                        entry.getKey() + " -> " + entry.getValue());
                first = false;
            }
        }

        return row;
    }

    // --- Commit handlers ---
    // Each checks if the value actually changed before pushing undo,
    // fixing the double-commit bug (Enter + focus-loss).
    // All use currentElementName, fixing the stale-name bug after renames.

    /**
     * Wires both onAction and focusedProperty listeners to a commit handler.
     */
    private void addCommitHandlers(TextField field,
                                    java.util.function.Consumer<TextField> handler) {
        field.setOnAction(e -> handler.accept(field));
        field.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused && !updatingFields) {
                handler.accept(field);
            }
        });
    }

    private void commitRename(TextField nameField) {
        String oldName = currentElementName;
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
        currentElementName = newName;
    }

    private void commitStockInitialValue(TextField field) {
        try {
            double value = Double.parseDouble(field.getText().trim());
            StockDef stock = editor.getStockByName(currentElementName);
            if (stock == null || stock.initialValue() == value) {
                return;
            }
            canvas.applyStockInitialValue(currentElementName, value);
        } catch (NumberFormatException ignored) {
            StockDef stock = editor.getStockByName(currentElementName);
            if (stock != null) {
                field.setText(ElementRenderer.formatValue(stock.initialValue()));
            }
        }
    }

    private void commitStockUnit(TextField field) {
        String unit = field.getText().trim();
        StockDef stock = editor.getStockByName(currentElementName);
        if (stock != null && unit.equals(stock.unit())) {
            return;
        }
        canvas.applyStockUnit(currentElementName, unit);
    }

    private void commitFlowEquation(TextField field) {
        String equation = field.getText().trim();
        if (equation.isEmpty()) {
            FlowDef flow = editor.getFlowByName(currentElementName);
            if (flow != null) {
                field.setText(flow.equation());
            }
            return;
        }
        FlowDef flow = editor.getFlowByName(currentElementName);
        if (flow != null && equation.equals(flow.equation())) {
            return;
        }
        canvas.applyFlowEquation(currentElementName, equation);
    }

    private void commitFlowTimeUnit(TextField field) {
        String timeUnit = field.getText().trim();
        if (timeUnit.isEmpty()) {
            FlowDef flow = editor.getFlowByName(currentElementName);
            if (flow != null) {
                field.setText(flow.timeUnit());
            }
            return;
        }
        FlowDef flow = editor.getFlowByName(currentElementName);
        if (flow != null && timeUnit.equals(flow.timeUnit())) {
            return;
        }
        canvas.applyFlowTimeUnit(currentElementName, timeUnit);
    }

    private void commitAuxEquation(TextField field) {
        String equation = field.getText().trim();
        if (equation.isEmpty()) {
            AuxDef aux = editor.getAuxByName(currentElementName);
            if (aux != null) {
                field.setText(aux.equation());
            }
            return;
        }
        AuxDef aux = editor.getAuxByName(currentElementName);
        if (aux != null && equation.equals(aux.equation())) {
            return;
        }
        canvas.applyAuxEquation(currentElementName, equation);
    }

    private void commitAuxUnit(TextField field) {
        String unit = field.getText().trim();
        AuxDef aux = editor.getAuxByName(currentElementName);
        if (aux != null && unit.equals(aux.unit())) {
            return;
        }
        canvas.applyAuxUnit(currentElementName, unit);
    }

    private void commitConstantValue(TextField field) {
        try {
            double value = Double.parseDouble(field.getText().trim());
            ConstantDef constant = editor.getConstantByName(currentElementName);
            if (constant == null || constant.value() == value) {
                return;
            }
            canvas.applyConstantValue(currentElementName, value);
        } catch (NumberFormatException ignored) {
            ConstantDef constant = editor.getConstantByName(currentElementName);
            if (constant != null) {
                field.setText(ElementRenderer.formatValue(constant.value()));
            }
        }
    }

    private void commitConstantUnit(TextField field) {
        String unit = field.getText().trim();
        ConstantDef constant = editor.getConstantByName(currentElementName);
        if (constant != null && unit.equals(constant.unit())) {
            return;
        }
        canvas.applyConstantUnit(currentElementName, unit);
    }

    // --- UI helpers ---

    private TextField createNameField(String currentName) {
        TextField nameField = createTextField(currentName);
        nameField.setId("nameField");
        nameField.setOnAction(e -> commitRename(nameField));
        nameField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused && !updatingFields) {
                commitRename(nameField);
            }
        });
        return nameField;
    }

    private TextField createTextField(String text) {
        TextField field = new TextField(text);
        field.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(field, Priority.ALWAYS);
        return field;
    }

    private void addFieldRow(int row, String labelText, javafx.scene.Node field) {
        Label label = new Label(labelText);
        label.setStyle(Styles.FIELD_LABEL);
        propertyGrid.add(label, 0, row);
        propertyGrid.add(field, 1, row);
    }

    private void addReadOnlyRow(int row, String labelText, String value) {
        Label label = new Label(labelText);
        label.setStyle(Styles.FIELD_LABEL);
        Label valueLabel = new Label(value);
        valueLabel.setStyle(Styles.SMALL_TEXT);
        valueLabel.setWrapText(true);
        propertyGrid.add(label, 0, row);
        propertyGrid.add(valueLabel, 1, row);
    }

    private static Button createToolbarButton(String text) {
        Button button = new Button(text);
        button.setStyle(Styles.SMALL_TEXT);
        return button;
    }

    private static String formatType(ElementType type) {
        if (type == null) {
            return "Unknown";
        }
        return switch (type) {
            case STOCK -> "Stock";
            case FLOW -> "Flow";
            case AUX -> "Auxiliary";
            case CONSTANT -> "Constant";
            case MODULE -> "Module";
            case LOOKUP -> "Lookup Table";
        };
    }
}

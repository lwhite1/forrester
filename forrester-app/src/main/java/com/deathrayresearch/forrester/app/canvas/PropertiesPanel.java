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
import java.util.Set;

/**
 * Right-side panel that displays properties of the currently selected canvas element.
 * Updates whenever the canvas selection changes. Contains a context toolbar with
 * action buttons and a form section with editable property fields.
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

        placeholderLabel.setStyle("-fx-text-fill: #7F8C8D; -fx-font-style: italic;");
        placeholderLabel.setPadding(new Insets(20));
        placeholderLabel.setAlignment(Pos.CENTER);
        placeholderLabel.setMaxWidth(Double.MAX_VALUE);

        showPlaceholder();
    }

    /**
     * Updates the panel to reflect the current selection on the canvas.
     * Called by ForresterApp whenever the canvas status changes.
     */
    public void updateSelection(ModelCanvas canvas, ModelEditor editor) {
        this.canvas = canvas;
        this.editor = editor;

        if (canvas == null || editor == null) {
            showPlaceholder();
            return;
        }

        Set<String> selection = canvas.getSelectedElementNames();

        if (selection.isEmpty()) {
            showPlaceholder();
        } else if (selection.size() == 1) {
            String name = selection.iterator().next();
            ElementType type = canvas.getSelectedElementType(name);
            showSingleElement(name, type);
        } else {
            showMultiSelection(selection.size());
        }
    }

    private void showPlaceholder() {
        getChildren().clear();
        getChildren().add(placeholderLabel);
    }

    private void showMultiSelection(int count) {
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
        propertyGrid.getRowCount();
        addReadOnlyRow(0, "Selection", count + " elements selected");

        getChildren().addAll(contextToolbar, separator, scrollPane);
    }

    private void showSingleElement(String name, ElementType type) {
        getChildren().clear();

        // Build context toolbar
        contextToolbar.getChildren().clear();

        Button renameBtn = createToolbarButton("Rename");
        renameBtn.setOnAction(e -> {
            // Focus the name field in the property grid if it exists
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
                    canvas.drillInto(name);
                    canvas.requestFocus();
                }
            });

            Button bindingsBtn = createToolbarButton("Bindings");
            bindingsBtn.setOnAction(e -> {
                if (canvas != null) {
                    canvas.triggerBindingConfig(name);
                    canvas.requestFocus();
                }
            });

            contextToolbar.getChildren().addAll(drillBtn, bindingsBtn);
        }

        // Build property form
        propertyGrid.getChildren().clear();
        int row = 0;

        // Type label (always shown, read-only)
        addReadOnlyRow(row++, "Type", formatType(type));

        if (type == null) {
            getChildren().addAll(contextToolbar, separator, scrollPane);
            return;
        }

        switch (type) {
            case STOCK -> row = buildStockForm(name, row);
            case FLOW -> row = buildFlowForm(name, row);
            case AUX -> row = buildAuxForm(name, row);
            case CONSTANT -> row = buildConstantForm(name, row);
            case MODULE -> row = buildModuleForm(name, row);
            default -> addReadOnlyRow(row++, "Name", name);
        }

        getChildren().addAll(contextToolbar, separator, scrollPane);
    }

    private int buildStockForm(String name, int row) {
        StockDef stock = editor.getStockByName(name);
        if (stock == null) {
            addReadOnlyRow(row++, "Name", name);
            return row;
        }

        // Name
        TextField nameField = createNameField(name);
        addFieldRow(row++, "Name", nameField);

        // Initial Value
        TextField initialValueField = createTextField(
                ElementRenderer.formatValue(stock.initialValue()));
        initialValueField.setOnAction(e -> commitStockInitialValue(name, initialValueField));
        initialValueField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused && !updatingFields) {
                commitStockInitialValue(name, initialValueField);
            }
        });
        addFieldRow(row++, "Initial Value", initialValueField);

        // Unit
        TextField unitField = createTextField(stock.unit() != null ? stock.unit() : "");
        unitField.setOnAction(e -> commitStockUnit(name, unitField));
        unitField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused && !updatingFields) {
                commitStockUnit(name, unitField);
            }
        });
        addFieldRow(row++, "Unit", unitField);

        // Negative Value Policy
        ComboBox<String> policyBox = new ComboBox<>();
        policyBox.getItems().addAll("Allow", "Clamp to Zero");
        String currentPolicy = stock.negativeValuePolicy();
        if ("CLAMP_TO_ZERO".equals(currentPolicy)) {
            policyBox.setValue("Clamp to Zero");
        } else {
            policyBox.setValue("Allow");
        }
        policyBox.setMaxWidth(Double.MAX_VALUE);
        policyBox.setOnAction(e -> {
            if (!updatingFields) {
                String policyValue = "Clamp to Zero".equals(policyBox.getValue())
                        ? "CLAMP_TO_ZERO" : null;
                canvas.pushUndoState();
                editor.setStockNegativeValuePolicy(name, policyValue);
                canvas.regenerateAndRedraw();
            }
        });
        addFieldRow(row++, "Policy", policyBox);

        return row;
    }

    private int buildFlowForm(String name, int row) {
        FlowDef flow = editor.getFlowByName(name);
        if (flow == null) {
            addReadOnlyRow(row++, "Name", name);
            return row;
        }

        // Name
        TextField nameField = createNameField(name);
        addFieldRow(row++, "Name", nameField);

        // Equation
        TextField equationField = createTextField(flow.equation());
        equationField.setOnAction(e -> commitFlowEquation(name, equationField));
        equationField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused && !updatingFields) {
                commitFlowEquation(name, equationField);
            }
        });
        addFieldRow(row++, "Equation", equationField);

        // Time Unit
        TextField timeUnitField = createTextField(
                flow.timeUnit() != null ? flow.timeUnit() : "");
        timeUnitField.setOnAction(e -> commitFlowTimeUnit(name, timeUnitField));
        timeUnitField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused && !updatingFields) {
                commitFlowTimeUnit(name, timeUnitField);
            }
        });
        addFieldRow(row++, "Time Unit", timeUnitField);

        // Source (read-only)
        String source = flow.source() != null ? flow.source() : "(cloud)";
        addReadOnlyRow(row++, "Source", source);

        // Sink (read-only)
        String sink = flow.sink() != null ? flow.sink() : "(cloud)";
        addReadOnlyRow(row++, "Sink", sink);

        return row;
    }

    private int buildAuxForm(String name, int row) {
        AuxDef aux = editor.getAuxByName(name);
        if (aux == null) {
            addReadOnlyRow(row++, "Name", name);
            return row;
        }

        // Name
        TextField nameField = createNameField(name);
        addFieldRow(row++, "Name", nameField);

        // Equation
        TextField equationField = createTextField(aux.equation());
        equationField.setOnAction(e -> commitAuxEquation(name, equationField));
        equationField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused && !updatingFields) {
                commitAuxEquation(name, equationField);
            }
        });
        addFieldRow(row++, "Equation", equationField);

        // Unit
        TextField unitField = createTextField(aux.unit() != null ? aux.unit() : "");
        unitField.setOnAction(e -> commitAuxUnit(name, unitField));
        unitField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused && !updatingFields) {
                commitAuxUnit(name, unitField);
            }
        });
        addFieldRow(row++, "Unit", unitField);

        return row;
    }

    private int buildConstantForm(String name, int row) {
        ConstantDef constant = editor.getConstantByName(name);
        if (constant == null) {
            addReadOnlyRow(row++, "Name", name);
            return row;
        }

        // Name
        TextField nameField = createNameField(name);
        addFieldRow(row++, "Name", nameField);

        // Value
        TextField valueField = createTextField(
                ElementRenderer.formatValue(constant.value()));
        valueField.setOnAction(e -> commitConstantValue(name, valueField));
        valueField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused && !updatingFields) {
                commitConstantValue(name, valueField);
            }
        });
        addFieldRow(row++, "Value", valueField);

        // Unit
        TextField unitField = createTextField(
                constant.unit() != null ? constant.unit() : "");
        unitField.setOnAction(e -> commitConstantUnit(name, unitField));
        unitField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused && !updatingFields) {
                commitConstantUnit(name, unitField);
            }
        });
        addFieldRow(row++, "Unit", unitField);

        return row;
    }

    private int buildModuleForm(String name, int row) {
        ModuleInstanceDef module = editor.getModuleByName(name);
        if (module == null) {
            addReadOnlyRow(row++, "Name", name);
            return row;
        }

        // Instance Name
        TextField nameField = createNameField(name);
        addFieldRow(row++, "Instance Name", nameField);

        // Input Bindings (read-only)
        Map<String, String> inputs = module.inputBindings();
        if (inputs.isEmpty()) {
            addReadOnlyRow(row++, "Inputs", "(none)");
        } else {
            boolean first = true;
            for (Map.Entry<String, String> entry : inputs.entrySet()) {
                String label = first ? "Inputs" : "";
                addReadOnlyRow(row++, label, entry.getKey() + " = " + entry.getValue());
                first = false;
            }
        }

        // Output Bindings (read-only)
        Map<String, String> outputs = module.outputBindings();
        if (outputs.isEmpty()) {
            addReadOnlyRow(row++, "Outputs", "(none)");
        } else {
            boolean first = true;
            for (Map.Entry<String, String> entry : outputs.entrySet()) {
                String label = first ? "Outputs" : "";
                addReadOnlyRow(row++, label, entry.getKey() + " -> " + entry.getValue());
                first = false;
            }
        }

        return row;
    }

    // --- Commit helpers ---

    private void commitRename(String oldName, TextField nameField) {
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
    }

    private void commitStockInitialValue(String name, TextField field) {
        try {
            double value = Double.parseDouble(field.getText().trim());
            canvas.pushUndoState();
            editor.setStockInitialValue(name, value);
            canvas.regenerateAndRedraw();
        } catch (NumberFormatException ignored) {
            // Revert to current value
            StockDef stock = editor.getStockByName(name);
            if (stock != null) {
                field.setText(ElementRenderer.formatValue(stock.initialValue()));
            }
        }
    }

    private void commitStockUnit(String name, TextField field) {
        String unit = field.getText().trim();
        canvas.pushUndoState();
        editor.setStockUnit(name, unit);
        canvas.regenerateAndRedraw();
    }

    private void commitFlowEquation(String name, TextField field) {
        String equation = field.getText().trim();
        if (!equation.isEmpty()) {
            canvas.pushUndoState();
            editor.setFlowEquation(name, equation);
            canvas.regenerateAndRedraw();
        } else {
            FlowDef flow = editor.getFlowByName(name);
            if (flow != null) {
                field.setText(flow.equation());
            }
        }
    }

    private void commitFlowTimeUnit(String name, TextField field) {
        String timeUnit = field.getText().trim();
        if (!timeUnit.isEmpty()) {
            canvas.pushUndoState();
            editor.setFlowTimeUnit(name, timeUnit);
            canvas.regenerateAndRedraw();
        } else {
            FlowDef flow = editor.getFlowByName(name);
            if (flow != null) {
                field.setText(flow.timeUnit());
            }
        }
    }

    private void commitAuxEquation(String name, TextField field) {
        String equation = field.getText().trim();
        if (!equation.isEmpty()) {
            canvas.pushUndoState();
            editor.setAuxEquation(name, equation);
            canvas.regenerateAndRedraw();
        } else {
            AuxDef aux = editor.getAuxByName(name);
            if (aux != null) {
                field.setText(aux.equation());
            }
        }
    }

    private void commitAuxUnit(String name, TextField field) {
        String unit = field.getText().trim();
        canvas.pushUndoState();
        editor.setAuxUnit(name, unit);
        canvas.regenerateAndRedraw();
    }

    private void commitConstantValue(String name, TextField field) {
        try {
            double value = Double.parseDouble(field.getText().trim());
            canvas.pushUndoState();
            editor.setConstantValue(name, value);
            canvas.regenerateAndRedraw();
        } catch (NumberFormatException ignored) {
            ConstantDef constant = editor.getConstantByName(name);
            if (constant != null) {
                field.setText(ElementRenderer.formatValue(constant.value()));
            }
        }
    }

    private void commitConstantUnit(String name, TextField field) {
        String unit = field.getText().trim();
        canvas.pushUndoState();
        editor.setConstantUnit(name, unit);
        canvas.regenerateAndRedraw();
    }

    // --- UI helpers ---

    private TextField createNameField(String currentName) {
        TextField nameField = createTextField(currentName);
        nameField.setId("nameField");
        nameField.setOnAction(e -> commitRename(currentName, nameField));
        nameField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused && !updatingFields) {
                commitRename(currentName, nameField);
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
        label.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");
        propertyGrid.add(label, 0, row);
        propertyGrid.add(field, 1, row);
    }

    private void addReadOnlyRow(int row, String labelText, String value) {
        Label label = new Label(labelText);
        label.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");
        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-size: 11px;");
        valueLabel.setWrapText(true);
        propertyGrid.add(label, 0, row);
        propertyGrid.add(valueLabel, 1, row);
    }

    private static Button createToolbarButton(String text) {
        Button button = new Button(text);
        button.setStyle("-fx-font-size: 11px;");
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

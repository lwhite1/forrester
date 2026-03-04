package com.deathrayresearch.forrester.app.canvas;

import com.deathrayresearch.forrester.model.def.StockDef;

import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

import java.util.Objects;

/**
 * Property form for stock elements. Builds editable fields for name,
 * initial value, unit, and negative value policy.
 */
class StockForm implements ElementForm {

    private final FormContext ctx;

    private TextField nameField;
    private TextField initialValueField;
    private ComboBox<String> unitBox;
    private ComboBox<String> policyBox;

    StockForm(FormContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public int build(int startRow) {
        StockDef stock = ctx.editor.getStockByName(ctx.elementName);
        if (stock == null) {
            ctx.addReadOnlyRow(startRow++, "Name", ctx.elementName);
            return startRow;
        }

        int row = startRow;
        nameField = ctx.createNameField();
        ctx.addFieldRow(row++, "Name", nameField,
                "The name used to reference this stock in equations");

        initialValueField = ctx.createTextField(
                ElementRenderer.formatValue(stock.initialValue()));
        ctx.addCommitHandlers(initialValueField, this::commitInitialValue);
        ctx.addFieldRow(row++, "Initial Value", initialValueField,
                "The starting value at the beginning of the simulation");

        unitBox = ctx.createUnitComboBox(stock.unit());
        ctx.addComboCommitHandlers(unitBox, this::commitUnit);
        ctx.addFieldRow(row++, "Unit", unitBox,
                "The unit of measurement (e.g., Person, Kilogram, USD)");

        policyBox = new ComboBox<>();
        policyBox.getItems().addAll("Clamp to Zero", "Allow");
        policyBox.setValue(policyDisplayValue(stock.negativeValuePolicy()));
        policyBox.setMaxWidth(Double.MAX_VALUE);
        policyBox.setOnAction(e -> {
            if (!ctx.updatingFields) {
                commitPolicy();
            }
        });
        ctx.addFieldRow(row++, "Negative Values", policyBox,
                "What happens when the stock goes below zero.\n"
                + "'Clamp to Zero' prevents negative values (common for physical "
                + "quantities like population or inventory).\n"
                + "'Allow' permits negatives (e.g., bank balances, temperature deltas).");

        return row;
    }

    @Override
    public void updateValues() {
        StockDef stock = ctx.editor.getStockByName(ctx.elementName);
        if (stock == null || nameField == null) {
            return;
        }
        nameField.setText(ctx.elementName);
        initialValueField.setText(
                ElementRenderer.formatValue(stock.initialValue()));
        unitBox.setValue(stock.unit() != null ? stock.unit() : "");
        policyBox.setValue(policyDisplayValue(stock.negativeValuePolicy()));
    }

    private void commitInitialValue(TextField field) {
        try {
            double value = Double.parseDouble(field.getText().trim());
            StockDef stock = ctx.editor.getStockByName(ctx.elementName);
            if (stock == null || stock.initialValue() == value) {
                return;
            }
            ctx.canvas.applyStockInitialValue(ctx.elementName, value);
        } catch (NumberFormatException ignored) {
            StockDef stock = ctx.editor.getStockByName(ctx.elementName);
            if (stock != null) {
                field.setText(ElementRenderer.formatValue(stock.initialValue()));
            }
        }
    }

    private void commitUnit(ComboBox<String> box) {
        String unit = box.getValue() != null ? box.getValue().trim() : "";
        StockDef stock = ctx.editor.getStockByName(ctx.elementName);
        if (stock != null && unit.equals(stock.unit())) {
            return;
        }
        ctx.canvas.applyStockUnit(ctx.elementName, unit);
    }

    private void commitPolicy() {
        String policyValue = "Allow".equals(policyBox.getValue())
                ? "ALLOW" : "CLAMP_TO_ZERO";
        StockDef stock = ctx.editor.getStockByName(ctx.elementName);
        if (stock != null && Objects.equals(policyValue,
                stock.negativeValuePolicy() != null ? stock.negativeValuePolicy() : "CLAMP_TO_ZERO")) {
            return;
        }
        ctx.canvas.applyStockNegativeValuePolicy(ctx.elementName, policyValue);
    }

    /**
     * Maps the stored policy string to a display value.
     * Null and "CLAMP_TO_ZERO" both display as "Clamp to Zero" (the engine default).
     */
    private static String policyDisplayValue(String policy) {
        if ("ALLOW".equals(policy)) {
            return "Allow";
        }
        return "Clamp to Zero";
    }
}

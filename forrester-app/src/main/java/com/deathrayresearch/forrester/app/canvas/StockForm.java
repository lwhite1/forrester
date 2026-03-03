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
    private TextField unitField;
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
        ctx.addFieldRow(row++, "Name", nameField);

        initialValueField = ctx.createTextField(
                ElementRenderer.formatValue(stock.initialValue()));
        ctx.addCommitHandlers(initialValueField, this::commitInitialValue);
        ctx.addFieldRow(row++, "Initial Value", initialValueField);

        unitField = ctx.createTextField(stock.unit() != null ? stock.unit() : "");
        ctx.addCommitHandlers(unitField, this::commitUnit);
        ctx.addFieldRow(row++, "Unit", unitField);

        policyBox = new ComboBox<>();
        policyBox.getItems().addAll("Allow", "Clamp to Zero");
        if ("CLAMP_TO_ZERO".equals(stock.negativeValuePolicy())) {
            policyBox.setValue("Clamp to Zero");
        } else {
            policyBox.setValue("Allow");
        }
        policyBox.setMaxWidth(Double.MAX_VALUE);
        policyBox.setOnAction(e -> {
            if (!ctx.updatingFields) {
                String policyValue = "Clamp to Zero".equals(policyBox.getValue())
                        ? "CLAMP_TO_ZERO" : null;
                StockDef s = ctx.editor.getStockByName(ctx.elementName);
                if (s != null && Objects.equals(policyValue, s.negativeValuePolicy())) {
                    return;
                }
                ctx.canvas.applyStockNegativeValuePolicy(ctx.elementName, policyValue);
            }
        });
        ctx.addFieldRow(row++, "Policy", policyBox);

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
        unitField.setText(stock.unit() != null ? stock.unit() : "");
        if ("CLAMP_TO_ZERO".equals(stock.negativeValuePolicy())) {
            policyBox.setValue("Clamp to Zero");
        } else {
            policyBox.setValue("Allow");
        }
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

    private void commitUnit(TextField field) {
        String unit = field.getText().trim();
        StockDef stock = ctx.editor.getStockByName(ctx.elementName);
        if (stock != null && unit.equals(stock.unit())) {
            return;
        }
        ctx.canvas.applyStockUnit(ctx.elementName, unit);
    }
}

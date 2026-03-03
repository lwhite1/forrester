package com.deathrayresearch.forrester.app.canvas;

import com.deathrayresearch.forrester.model.def.ConstantDef;

import javafx.scene.control.TextField;

/**
 * Property form for constant elements. Builds editable fields
 * for name, value, and unit.
 */
class ConstantForm implements ElementForm {

    private final FormContext ctx;

    private TextField nameField;
    private TextField valueField;
    private TextField unitField;

    ConstantForm(FormContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public int build(int startRow) {
        ConstantDef constant = ctx.editor.getConstantByName(ctx.elementName);
        if (constant == null) {
            ctx.addReadOnlyRow(startRow++, "Name", ctx.elementName);
            return startRow;
        }

        int row = startRow;
        nameField = ctx.createNameField();
        ctx.addFieldRow(row++, "Name", nameField);

        valueField = ctx.createTextField(
                ElementRenderer.formatValue(constant.value()));
        ctx.addCommitHandlers(valueField, this::commitValue);
        ctx.addFieldRow(row++, "Value", valueField);

        unitField = ctx.createTextField(
                constant.unit() != null ? constant.unit() : "");
        ctx.addCommitHandlers(unitField, this::commitUnit);
        ctx.addFieldRow(row++, "Unit", unitField);

        return row;
    }

    @Override
    public void updateValues() {
        ConstantDef constant = ctx.editor.getConstantByName(ctx.elementName);
        if (constant == null || nameField == null) {
            return;
        }
        nameField.setText(ctx.elementName);
        valueField.setText(ElementRenderer.formatValue(constant.value()));
        unitField.setText(constant.unit() != null ? constant.unit() : "");
    }

    private void commitValue(TextField field) {
        try {
            double value = Double.parseDouble(field.getText().trim());
            ConstantDef constant = ctx.editor.getConstantByName(ctx.elementName);
            if (constant == null || constant.value() == value) {
                return;
            }
            ctx.canvas.applyConstantValue(ctx.elementName, value);
        } catch (NumberFormatException ignored) {
            ConstantDef constant = ctx.editor.getConstantByName(ctx.elementName);
            if (constant != null) {
                field.setText(ElementRenderer.formatValue(constant.value()));
            }
        }
    }

    private void commitUnit(TextField field) {
        String unit = field.getText().trim();
        ConstantDef constant = ctx.editor.getConstantByName(ctx.elementName);
        if (constant != null && unit.equals(constant.unit())) {
            return;
        }
        ctx.canvas.applyConstantUnit(ctx.elementName, unit);
    }
}

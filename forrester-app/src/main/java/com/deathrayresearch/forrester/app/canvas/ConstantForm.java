package com.deathrayresearch.forrester.app.canvas;

import com.deathrayresearch.forrester.model.def.ConstantDef;

import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

/**
 * Property form for constant elements. Builds editable fields
 * for name, value, and unit.
 */
class ConstantForm implements ElementForm {

    private final FormContext ctx;

    private TextField nameField;
    private TextField valueField;
    private ComboBox<String> unitBox;

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
        ctx.addFieldRow(row++, "Name", nameField,
                "The name used to reference this constant in equations");

        valueField = ctx.createTextField(
                ElementRenderer.formatValue(constant.value()));
        ctx.addCommitHandlers(valueField, this::commitValue);
        ctx.addFieldRow(row++, "Value", valueField,
                "A fixed numeric value that does not change during simulation");

        unitBox = ctx.createUnitComboBox(constant.unit());
        ctx.addComboCommitHandlers(unitBox, this::commitUnit);
        ctx.addFieldRow(row++, "Unit", unitBox,
                "The unit of measurement");

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
        unitBox.setValue(constant.unit() != null ? constant.unit() : "");
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

    private void commitUnit(ComboBox<String> box) {
        String unit = box.getValue() != null ? box.getValue().trim() : "";
        ConstantDef constant = ctx.editor.getConstantByName(ctx.elementName);
        if (constant != null && unit.equals(constant.unit())) {
            return;
        }
        ctx.canvas.applyConstantUnit(ctx.elementName, unit);
    }
}

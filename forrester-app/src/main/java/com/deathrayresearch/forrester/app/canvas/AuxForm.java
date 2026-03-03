package com.deathrayresearch.forrester.app.canvas;

import com.deathrayresearch.forrester.model.def.AuxDef;

import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

/**
 * Property form for auxiliary variable elements. Builds editable fields
 * for name, equation, and unit.
 */
class AuxForm implements ElementForm {

    private final FormContext ctx;

    private TextField nameField;
    private TextField equationField;
    private ComboBox<String> unitBox;

    AuxForm(FormContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public int build(int startRow) {
        AuxDef aux = ctx.editor.getAuxByName(ctx.elementName);
        if (aux == null) {
            ctx.addReadOnlyRow(startRow++, "Name", ctx.elementName);
            return startRow;
        }

        int row = startRow;
        nameField = ctx.createNameField();
        ctx.addFieldRow(row++, "Name", nameField);

        equationField = ctx.createTextField(aux.equation());
        ctx.addCommitHandlers(equationField, this::commitEquation);
        EquationAutoComplete.attach(equationField, ctx.editor, ctx.elementName);
        ctx.addFieldRow(row++, "Equation", equationField);

        unitBox = ctx.createUnitComboBox(aux.unit());
        ctx.addComboCommitHandlers(unitBox, this::commitUnit);
        ctx.addFieldRow(row++, "Unit", unitBox);

        return row;
    }

    @Override
    public void updateValues() {
        AuxDef aux = ctx.editor.getAuxByName(ctx.elementName);
        if (aux == null || nameField == null) {
            return;
        }
        nameField.setText(ctx.elementName);
        equationField.setText(aux.equation());
        unitBox.setValue(aux.unit() != null ? aux.unit() : "");
    }

    @Override
    public void dispose() {
        EquationAutoComplete.detach(equationField);
    }

    private void commitEquation(TextField field) {
        String equation = field.getText().trim();
        if (equation.isEmpty()) {
            AuxDef aux = ctx.editor.getAuxByName(ctx.elementName);
            if (aux != null) {
                field.setText(aux.equation());
            }
            return;
        }
        AuxDef aux = ctx.editor.getAuxByName(ctx.elementName);
        if (aux != null && equation.equals(aux.equation())) {
            return;
        }
        ctx.canvas.applyAuxEquation(ctx.elementName, equation);
    }

    private void commitUnit(ComboBox<String> box) {
        String unit = box.getValue() != null ? box.getValue().trim() : "";
        AuxDef aux = ctx.editor.getAuxByName(ctx.elementName);
        if (aux != null && unit.equals(aux.unit())) {
            return;
        }
        ctx.canvas.applyAuxUnit(ctx.elementName, unit);
    }
}

package com.deathrayresearch.forrester.app.canvas;

import com.deathrayresearch.forrester.model.def.FlowDef;

import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

/**
 * Property form for flow elements. Builds editable fields for name,
 * equation, and time unit, plus read-only source/sink labels.
 */
class FlowForm implements ElementForm {

    private final FormContext ctx;

    private TextField nameField;
    private TextField equationField;
    private ComboBox<String> timeUnitBox;
    private Label sourceLabel;
    private Label sinkLabel;

    FlowForm(FormContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public int build(int startRow) {
        FlowDef flow = ctx.editor.getFlowByName(ctx.elementName);
        if (flow == null) {
            ctx.addReadOnlyRow(startRow++, "Name", ctx.elementName);
            return startRow;
        }

        int row = startRow;
        nameField = ctx.createNameField();
        ctx.addFieldRow(row++, "Name", nameField,
                "The name used to reference this flow in equations");

        equationField = ctx.createTextField(flow.equation());
        ctx.addCommitHandlers(equationField, this::commitEquation);
        EquationAutoComplete.attach(equationField, ctx.editor, ctx.elementName);
        ctx.addFieldRow(row++, "Equation", equationField,
                "The rate equation determining how fast material flows.\n"
                + "Use element names, operators (+, -, *, /), and functions.");

        timeUnitBox = ctx.createTimeUnitComboBox(flow.timeUnit());
        ctx.addComboCommitHandlers(timeUnitBox, this::commitTimeUnit);
        ctx.addFieldRow(row++, "Time Unit", timeUnitBox,
                "The time unit for the flow rate (e.g., units per Day)");

        sourceLabel = new Label(flow.source() != null ? flow.source() : "(cloud)");
        ctx.addReadOnlyRow(row++, "Source", sourceLabel,
                "The stock this flow drains from. (cloud) = unlimited external source.");
        sinkLabel = new Label(flow.sink() != null ? flow.sink() : "(cloud)");
        ctx.addReadOnlyRow(row++, "Sink", sinkLabel,
                "The stock this flow fills. (cloud) = unlimited external sink.");

        return row;
    }

    @Override
    public void updateValues() {
        FlowDef flow = ctx.editor.getFlowByName(ctx.elementName);
        if (flow == null || nameField == null) {
            return;
        }
        nameField.setText(ctx.elementName);
        equationField.setText(flow.equation());
        timeUnitBox.setValue(flow.timeUnit() != null ? flow.timeUnit() : "");
        sourceLabel.setText(flow.source() != null ? flow.source() : "(cloud)");
        sinkLabel.setText(flow.sink() != null ? flow.sink() : "(cloud)");
    }

    @Override
    public void dispose() {
        EquationAutoComplete.detach(equationField);
    }

    private void commitEquation(TextField field) {
        String equation = field.getText().trim();
        if (equation.isEmpty()) {
            FlowDef flow = ctx.editor.getFlowByName(ctx.elementName);
            if (flow != null) {
                field.setText(flow.equation());
            }
            return;
        }
        FlowDef flow = ctx.editor.getFlowByName(ctx.elementName);
        if (flow != null && equation.equals(flow.equation())) {
            return;
        }
        ctx.canvas.applyFlowEquation(ctx.elementName, equation);
    }

    private void commitTimeUnit(ComboBox<String> box) {
        String timeUnit = box.getValue() != null ? box.getValue().trim() : "";
        if (timeUnit.isEmpty()) {
            FlowDef flow = ctx.editor.getFlowByName(ctx.elementName);
            if (flow != null) {
                box.setValue(flow.timeUnit());
            }
            return;
        }
        FlowDef flow = ctx.editor.getFlowByName(ctx.elementName);
        if (flow != null && timeUnit.equals(flow.timeUnit())) {
            return;
        }
        ctx.canvas.applyFlowTimeUnit(ctx.elementName, timeUnit);
    }
}

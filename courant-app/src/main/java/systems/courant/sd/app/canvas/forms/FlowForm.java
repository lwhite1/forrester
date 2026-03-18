package systems.courant.sd.app.canvas.forms;

import systems.courant.sd.model.def.FlowDef;

import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

import java.util.Objects;
import java.util.Optional;
import systems.courant.sd.app.canvas.EquationAutoComplete;
import systems.courant.sd.app.canvas.EquationField;

/**
 * Property form for flow elements. Builds editable fields for name,
 * equation, and time unit, plus read-only source/sink labels.
 */
public class FlowForm implements ElementForm {

    private final FormContext ctx;

    private TextField nameField;
    private EquationField equationField;
    private ComboBox<String> materialUnitBox;
    private ComboBox<String> timeUnitBox;
    private Label sourceLabel;
    private Label sinkLabel;
    private TextArea commentArea;

    public FlowForm(FormContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public int build(int startRow) {
        Optional<FlowDef> flowOpt = ctx.getEditor().getFlowByName(ctx.getElementName());
        if (flowOpt.isEmpty()) {
            ctx.addReadOnlyRow(startRow++, "Name", ctx.getElementName());
            return startRow;
        }
        FlowDef flow = flowOpt.get();

        int row = startRow;
        nameField = ctx.createNameField();
        ctx.addFieldRow(row++, "Name", nameField,
                "The name used to reference this flow in equations");

        commentArea = ctx.addCommentArea(row++, flow.comment(), this::commitComment);

        equationField = ctx.createEquationField(flow.equation());
        ctx.addEquationCommitHandlers(equationField, this::commitEquation);
        EquationAutoComplete.attach(equationField, ctx.getEditor(), ctx.getElementName());
        ctx.addFieldRow(row++, "Equation", ctx.wrapWithHelpButton(equationField),
                "The rate equation determining how fast material flows.\n"
                + "Use element names, operators (+, -, *, /), and functions.");
        ctx.attachEquationValidation(equationField, row++);

        materialUnitBox = ctx.createUnitComboBox(flow.materialUnit());
        ctx.addComboCommitHandlers(materialUnitBox, this::commitMaterialUnit);
        ctx.addFieldRow(row++, "Material Unit", materialUnitBox,
                "The material being transferred (e.g., Person, USD).\n"
                + "Combined with Time Unit to form the rate (e.g., Person per Day).");

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
        Optional<FlowDef> flowOpt = ctx.getEditor().getFlowByName(ctx.getElementName());
        if (flowOpt.isEmpty() || nameField == null) {
            return;
        }
        FlowDef flow = flowOpt.get();
        nameField.setText(ctx.getElementName());
        equationField.setText(flow.equation());
        materialUnitBox.setValue(flow.materialUnit() != null ? flow.materialUnit() : "");
        timeUnitBox.setValue(flow.timeUnit() != null ? flow.timeUnit() : "");
        sourceLabel.setText(flow.source() != null ? flow.source() : "(cloud)");
        sinkLabel.setText(flow.sink() != null ? flow.sink() : "(cloud)");
        commentArea.setText(flow.comment() != null ? flow.comment() : "");
    }

    @Override
    public void dispose() {
        EquationAutoComplete.detach(equationField);
    }

    private void commitComment(TextArea area) {
        String text = area.getText().trim();
        String comment = text.isEmpty() ? null : text;
        Optional<FlowDef> flowOpt = ctx.getEditor().getFlowByName(ctx.getElementName());
        if (flowOpt.isEmpty() || Objects.equals(comment, flowOpt.get().comment())) {
            return;
        }
        ctx.getCanvas().applyMutation(() -> ctx.getEditor().setFlowComment(ctx.getElementName(), comment));
    }

    private void commitEquation(EquationField field) {
        String equation = field.getText().trim();
        if (equation.isEmpty()) {
            ctx.getEditor().getFlowByName(ctx.getElementName())
                    .ifPresent(flow -> field.setText(flow.equation()));
            return;
        }
        Optional<FlowDef> flowOpt = ctx.getEditor().getFlowByName(ctx.getElementName());
        if (flowOpt.isPresent() && equation.equals(flowOpt.get().equation())) {
            return;
        }
        ctx.getCanvas().applyMutation(() -> ctx.getEditor().setFlowEquation(ctx.getElementName(), equation));
    }

    private void commitMaterialUnit(ComboBox<String> box) {
        String materialUnit = box.getValue() != null ? box.getValue().trim() : "";
        String resolved = materialUnit.isEmpty() ? null : materialUnit;
        Optional<FlowDef> flowOpt = ctx.getEditor().getFlowByName(ctx.getElementName());
        if (flowOpt.isPresent() && Objects.equals(resolved, flowOpt.get().materialUnit())) {
            return;
        }
        ctx.getCanvas().applyMutation(
                () -> ctx.getEditor().setFlowMaterialUnit(ctx.getElementName(), resolved));
    }

    private void commitTimeUnit(ComboBox<String> box) {
        String timeUnit = box.getValue() != null ? box.getValue().trim() : "";
        if (timeUnit.isEmpty()) {
            ctx.getEditor().getFlowByName(ctx.getElementName())
                    .ifPresent(flow -> box.setValue(flow.timeUnit()));
            return;
        }
        Optional<FlowDef> flowOpt = ctx.getEditor().getFlowByName(ctx.getElementName());
        if (flowOpt.isPresent() && timeUnit.equals(flowOpt.get().timeUnit())) {
            return;
        }
        ctx.getCanvas().applyMutation(() -> ctx.getEditor().setFlowTimeUnit(ctx.getElementName(), timeUnit));
    }
}

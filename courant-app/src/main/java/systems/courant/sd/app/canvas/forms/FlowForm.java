package systems.courant.sd.app.canvas.forms;

import systems.courant.sd.model.def.FlowDef;

import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.util.List;
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
    private final FormFieldBuilder fields;
    private final DimensionalAnalysisUI dimAnalysis;

    private TextField nameField;
    private EquationField equationField;
    private ComboBox<String> materialUnitBox;
    private ComboBox<String> timeUnitBox;
    private Label sourceLabel;
    private Label sinkLabel;
    private TextArea commentArea;

    public FlowForm(FormContext ctx, FormFieldBuilder fields, DimensionalAnalysisUI dimAnalysis) {
        this.ctx = ctx;
        this.fields = fields;
        this.dimAnalysis = dimAnalysis;
    }

    @Override
    public int build(int startRow) {
        Optional<FlowDef> flowOpt = ctx.getEditor().getFlowByName(ctx.getElementName());
        if (flowOpt.isEmpty()) {
            fields.addReadOnlyRow(startRow++, "Name", ctx.getElementName());
            return startRow;
        }
        FlowDef flow = flowOpt.get();

        int row = startRow;
        nameField = fields.createNameField();
        fields.addFieldRow(row++, "Name", nameField,
                "The name used to reference this flow in equations");

        commentArea = fields.addCommentArea(row++, flow.comment(), this::commitComment);

        equationField = fields.createEquationField(flow.equation());
        fields.addEquationCommitHandlers(equationField, this::commitEquation);
        EquationAutoComplete.attach(equationField, ctx.getEditor(), ctx.getElementName());
        fields.addFieldRow(row++, "Equation", fields.wrapWithHelpButton(equationField),
                "The rate equation determining how fast material flows.\n"
                + "Use element names, operators (+, -, *, /), and functions.");
        dimAnalysis.attachEquationValidation(equationField, row++);

        materialUnitBox = fields.createUnitComboBox(flow.materialUnit());
        fields.addComboCommitHandlers(materialUnitBox, this::commitMaterialUnit);
        fields.addFieldRow(row++, "Material Unit", materialUnitBox,
                "The material being transferred (e.g., Person, USD).\n"
                + "Combined with Time Unit to form the rate (e.g., Person per Day).");

        timeUnitBox = fields.createTimeUnitComboBox(flow.timeUnit());
        fields.addComboCommitHandlers(timeUnitBox, this::commitTimeUnit);
        fields.addFieldRow(row++, "Time Unit", timeUnitBox,
                "The time unit for the flow rate (e.g., units per Day)");

        sourceLabel = new Label(flow.source() != null ? flow.source() : "(cloud)");
        fields.addReadOnlyRow(row++, "Source", sourceLabel,
                "The stock this flow drains from. (cloud) = unlimited external source.");
        sinkLabel = new Label(flow.sink() != null ? flow.sink() : "(cloud)");
        fields.addReadOnlyRow(row++, "Sink", sinkLabel,
                "The stock this flow fills. (cloud) = unlimited external sink.");

        row = fields.addSubscriptRow(row, ctx.getEditor().getSubscripts(),
                flow.subscripts(), this::commitSubscripts);

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
        dimAnalysis.revalidate();
    }

    @Override
    public void dispose() {
        EquationAutoComplete.detach(equationField);
    }

    private void commitComment(TextArea area) {
        String comment = FormFieldBuilder.normalizeToNull(area.getText());
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
        autoFillMaterialUnitIfBlank(equation);
    }

    private void autoFillMaterialUnitIfBlank(String equation) {
        Optional<FlowDef> flowOpt = ctx.getEditor().getFlowByName(ctx.getElementName());
        if (flowOpt.isEmpty()) {
            return;
        }
        FlowDef flow = flowOpt.get();
        if (flow.materialUnit() != null && !flow.materialUnit().isBlank()) {
            return;
        }
        String inferred = dimAnalysis.inferFlowMaterialUnit(equation, flow.timeUnit());
        if (inferred != null) {
            ctx.getCanvas().applyMutation(
                    () -> ctx.getEditor().setFlowMaterialUnit(ctx.getElementName(), inferred));
            materialUnitBox.setValue(inferred);
        }
    }

    private void commitMaterialUnit(ComboBox<String> box) {
        String resolved = FormFieldBuilder.normalizeToNull(box.getValue());
        Optional<FlowDef> flowOpt = ctx.getEditor().getFlowByName(ctx.getElementName());
        if (flowOpt.isPresent() && Objects.equals(resolved, flowOpt.get().materialUnit())) {
            return;
        }
        ctx.getCanvas().applyMutation(
                () -> ctx.getEditor().setFlowMaterialUnit(ctx.getElementName(), resolved));
    }

    private void commitSubscripts(List<String> subscripts) {
        Optional<FlowDef> flowOpt = ctx.getEditor().getFlowByName(ctx.getElementName());
        if (flowOpt.isEmpty() || flowOpt.get().subscripts().equals(subscripts)) {
            return;
        }
        ctx.getCanvas().applyMutation(
                () -> ctx.getEditor().setFlowSubscripts(ctx.getElementName(), subscripts));
    }

    private void commitTimeUnit(ComboBox<String> box) {
        String timeUnit = FormFieldBuilder.normalizeToNull(box.getValue());
        if (timeUnit == null) {
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

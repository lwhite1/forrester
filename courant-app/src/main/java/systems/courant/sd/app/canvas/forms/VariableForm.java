package systems.courant.sd.app.canvas.forms;

import systems.courant.sd.model.def.VariableDef;

import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import systems.courant.sd.app.canvas.EquationAutoComplete;
import systems.courant.sd.app.canvas.EquationField;

/**
 * Property form for variable elements. Builds editable fields
 * for name, equation, and unit.
 */
public class VariableForm implements ElementForm {

    private final FormContext ctx;
    private final FormFieldBuilder fields;
    private final DimensionalAnalysisUI dimAnalysis;

    private TextField nameField;
    private EquationField equationField;
    private ComboBox<String> unitBox;
    private TextArea commentArea;

    public VariableForm(FormContext ctx, FormFieldBuilder fields, DimensionalAnalysisUI dimAnalysis) {
        this.ctx = ctx;
        this.fields = fields;
        this.dimAnalysis = dimAnalysis;
    }

    @Override
    public int build(int startRow) {
        Optional<VariableDef> varOpt = ctx.getEditor().getVariableByName(ctx.getElementName());
        if (varOpt.isEmpty()) {
            fields.addReadOnlyRow(startRow++, "Name", ctx.getElementName());
            return startRow;
        }
        VariableDef v = varOpt.get();

        int row = startRow;
        nameField = fields.createNameField();
        fields.addFieldRow(row++, "Name", nameField,
                "The name used to reference this variable in equations");

        commentArea = fields.addCommentArea(row++, v.comment(), this::commitComment);

        equationField = fields.createEquationField(v.equation());
        fields.addEquationCommitHandlers(equationField, this::commitEquation);
        EquationAutoComplete.attach(equationField, ctx.getEditor(), ctx.getElementName());
        fields.addFieldRow(row++, "Equation", fields.wrapWithHelpButton(equationField),
                "A formula computed each time step from other model elements");
        dimAnalysis.attachEquationValidation(equationField, row++);

        unitBox = fields.createUnitComboBox(v.unit());
        fields.addComboCommitHandlers(unitBox, this::commitUnit);
        fields.addFieldRow(row++, "Unit", unitBox,
                "The unit of measurement");

        row = fields.addSubscriptRow(row, ctx.getEditor().getSubscripts(),
                v.subscripts(), this::commitSubscripts);

        return row;
    }

    @Override
    public void updateValues() {
        Optional<VariableDef> varOpt = ctx.getEditor().getVariableByName(ctx.getElementName());
        if (varOpt.isEmpty() || nameField == null) {
            return;
        }
        VariableDef v = varOpt.get();
        nameField.setText(ctx.getElementName());
        equationField.setText(v.equation());
        unitBox.setValue(v.unit() != null ? v.unit() : "");
        commentArea.setText(v.comment() != null ? v.comment() : "");
        dimAnalysis.revalidate();
    }

    @Override
    public void dispose() {
        EquationAutoComplete.detach(equationField);
    }

    private void commitComment(TextArea area) {
        String text = area.getText().trim();
        String comment = text.isEmpty() ? null : text;
        Optional<VariableDef> varOpt = ctx.getEditor().getVariableByName(ctx.getElementName());
        if (varOpt.isEmpty() || Objects.equals(comment, varOpt.get().comment())) {
            return;
        }
        ctx.getCanvas().applyMutation(() -> ctx.getEditor().setVariableComment(ctx.getElementName(), comment));
    }

    private void commitEquation(EquationField field) {
        String equation = field.getText().trim();
        if (equation.isEmpty()) {
            ctx.getEditor().getVariableByName(ctx.getElementName())
                    .ifPresent(v -> field.setText(v.equation()));
            return;
        }
        Optional<VariableDef> varOpt = ctx.getEditor().getVariableByName(ctx.getElementName());
        if (varOpt.isPresent() && equation.equals(varOpt.get().equation())) {
            return;
        }
        ctx.getCanvas().applyMutation(() -> ctx.getEditor().setVariableEquation(ctx.getElementName(), equation));
    }

    private void commitSubscripts(List<String> subscripts) {
        Optional<VariableDef> varOpt = ctx.getEditor().getVariableByName(ctx.getElementName());
        if (varOpt.isEmpty() || varOpt.get().subscripts().equals(subscripts)) {
            return;
        }
        ctx.getCanvas().applyMutation(
                () -> ctx.getEditor().setVariableSubscripts(ctx.getElementName(), subscripts));
    }

    private void commitUnit(ComboBox<String> box) {
        String raw = box.getValue() != null ? box.getValue().trim() : "";
        String unit = raw.isEmpty() ? null : raw;
        Optional<VariableDef> varOpt = ctx.getEditor().getVariableByName(ctx.getElementName());
        if (varOpt.isPresent() && Objects.equals(unit, varOpt.get().unit())) {
            return;
        }
        ctx.getCanvas().applyMutation(() -> ctx.getEditor().setVariableUnit(ctx.getElementName(), unit));
    }
}

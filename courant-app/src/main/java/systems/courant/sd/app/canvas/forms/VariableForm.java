package systems.courant.sd.app.canvas.forms;

import systems.courant.sd.model.def.VariableDef;

import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

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

    private TextField nameField;
    private EquationField equationField;
    private ComboBox<String> unitBox;
    private TextArea commentArea;

    public VariableForm(FormContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public int build(int startRow) {
        Optional<VariableDef> varOpt = ctx.getEditor().getVariableByName(ctx.getElementName());
        if (varOpt.isEmpty()) {
            ctx.addReadOnlyRow(startRow++, "Name", ctx.getElementName());
            return startRow;
        }
        VariableDef v = varOpt.get();

        int row = startRow;
        nameField = ctx.createNameField();
        ctx.addFieldRow(row++, "Name", nameField,
                "The name used to reference this variable in equations");

        commentArea = ctx.addCommentArea(row++, v.comment(), this::commitComment);

        equationField = ctx.createEquationField(v.equation());
        ctx.addEquationCommitHandlers(equationField, this::commitEquation);
        EquationAutoComplete.attach(equationField, ctx.getEditor(), ctx.getElementName());
        ctx.addFieldRow(row++, "Equation", ctx.wrapWithHelpButton(equationField),
                "A formula computed each time step from other model elements");
        ctx.attachEquationValidation(equationField, row++);

        unitBox = ctx.createUnitComboBox(v.unit());
        ctx.addComboCommitHandlers(unitBox, this::commitUnit);
        ctx.addFieldRow(row++, "Unit", unitBox,
                "The unit of measurement");

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

    private void commitUnit(ComboBox<String> box) {
        String unit = box.getValue() != null ? box.getValue().trim() : "";
        Optional<VariableDef> varOpt = ctx.getEditor().getVariableByName(ctx.getElementName());
        if (varOpt.isPresent() && unit.equals(varOpt.get().unit())) {
            return;
        }
        ctx.getCanvas().applyMutation(() -> ctx.getEditor().setVariableUnit(ctx.getElementName(), unit));
    }
}

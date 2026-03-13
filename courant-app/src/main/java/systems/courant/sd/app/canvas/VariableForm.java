package systems.courant.sd.app.canvas;

import systems.courant.sd.model.def.VariableDef;

import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

import java.util.Objects;
import java.util.Optional;

/**
 * Property form for variable elements. Builds editable fields
 * for name, equation, and unit.
 */
class VariableForm implements ElementForm {

    private final FormContext ctx;

    private TextField nameField;
    private EquationField equationField;
    private ComboBox<String> unitBox;
    private TextArea commentArea;

    VariableForm(FormContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public int build(int startRow) {
        Optional<VariableDef> varOpt = ctx.editor.getVariableByName(ctx.elementName);
        if (varOpt.isEmpty()) {
            ctx.addReadOnlyRow(startRow++, "Name", ctx.elementName);
            return startRow;
        }
        VariableDef v = varOpt.get();

        int row = startRow;
        nameField = ctx.createNameField();
        ctx.addFieldRow(row++, "Name", nameField,
                "The name used to reference this variable in equations");

        commentArea = new TextArea(v.comment() != null ? v.comment() : "");
        commentArea.setId("propComment");
        commentArea.setPrefRowCount(2);
        commentArea.setWrapText(true);
        commentArea.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(commentArea, Priority.ALWAYS);
        ctx.addTextAreaCommitHandlers(commentArea, this::commitComment);
        ctx.addFieldRow(row++, "Description", commentArea,
                "Documentation for this element");

        equationField = ctx.createEquationField(v.equation());
        ctx.addEquationCommitHandlers(equationField, this::commitEquation);
        EquationAutoComplete.attach(equationField, ctx.editor, ctx.elementName);
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
        Optional<VariableDef> varOpt = ctx.editor.getVariableByName(ctx.elementName);
        if (varOpt.isEmpty() || nameField == null) {
            return;
        }
        VariableDef v = varOpt.get();
        nameField.setText(ctx.elementName);
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
        Optional<VariableDef> varOpt = ctx.editor.getVariableByName(ctx.elementName);
        if (varOpt.isEmpty() || Objects.equals(comment, varOpt.get().comment())) {
            return;
        }
        ctx.canvas.applyMutation(() -> ctx.editor.setVariableComment(ctx.elementName, comment));
    }

    private void commitEquation(EquationField field) {
        String equation = field.getText().trim();
        if (equation.isEmpty()) {
            ctx.editor.getVariableByName(ctx.elementName)
                    .ifPresent(v -> field.setText(v.equation()));
            return;
        }
        Optional<VariableDef> varOpt = ctx.editor.getVariableByName(ctx.elementName);
        if (varOpt.isPresent() && equation.equals(varOpt.get().equation())) {
            return;
        }
        ctx.canvas.applyMutation(() -> ctx.editor.setVariableEquation(ctx.elementName, equation));
    }

    private void commitUnit(ComboBox<String> box) {
        String unit = box.getValue() != null ? box.getValue().trim() : "";
        Optional<VariableDef> varOpt = ctx.editor.getVariableByName(ctx.elementName);
        if (varOpt.isPresent() && unit.equals(varOpt.get().unit())) {
            return;
        }
        ctx.canvas.applyMutation(() -> ctx.editor.setVariableUnit(ctx.elementName, unit));
    }
}

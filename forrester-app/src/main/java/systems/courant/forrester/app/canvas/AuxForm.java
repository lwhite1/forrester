package systems.courant.forrester.app.canvas;

import systems.courant.forrester.model.def.AuxDef;

import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

import java.util.Objects;
import java.util.Optional;

/**
 * Property form for auxiliary variable elements. Builds editable fields
 * for name, equation, and unit.
 */
class AuxForm implements ElementForm {

    private final FormContext ctx;

    private TextField nameField;
    private EquationField equationField;
    private ComboBox<String> unitBox;
    private TextArea commentArea;

    AuxForm(FormContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public int build(int startRow) {
        Optional<AuxDef> auxOpt = ctx.editor.getAuxByName(ctx.elementName);
        if (auxOpt.isEmpty()) {
            ctx.addReadOnlyRow(startRow++, "Name", ctx.elementName);
            return startRow;
        }
        AuxDef aux = auxOpt.get();

        int row = startRow;
        nameField = ctx.createNameField();
        ctx.addFieldRow(row++, "Name", nameField,
                "The name used to reference this variable in equations");

        commentArea = new TextArea(aux.comment() != null ? aux.comment() : "");
        commentArea.setId("propComment");
        commentArea.setPrefRowCount(2);
        commentArea.setWrapText(true);
        commentArea.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(commentArea, Priority.ALWAYS);
        ctx.addTextAreaCommitHandlers(commentArea, this::commitComment);
        ctx.addFieldRow(row++, "Description", commentArea,
                "Documentation for this element");

        equationField = ctx.createEquationField(aux.equation());
        ctx.addEquationCommitHandlers(equationField, this::commitEquation);
        EquationAutoComplete.attach(equationField, ctx.editor, ctx.elementName);
        ctx.addFieldRow(row++, "Equation", ctx.wrapWithHelpButton(equationField),
                "A formula computed each time step from other model elements");
        ctx.attachEquationValidation(equationField, row++);

        unitBox = ctx.createUnitComboBox(aux.unit());
        ctx.addComboCommitHandlers(unitBox, this::commitUnit);
        ctx.addFieldRow(row++, "Unit", unitBox,
                "The unit of measurement");

        return row;
    }

    @Override
    public void updateValues() {
        Optional<AuxDef> auxOpt = ctx.editor.getAuxByName(ctx.elementName);
        if (auxOpt.isEmpty() || nameField == null) {
            return;
        }
        AuxDef aux = auxOpt.get();
        nameField.setText(ctx.elementName);
        equationField.setText(aux.equation());
        unitBox.setValue(aux.unit() != null ? aux.unit() : "");
        commentArea.setText(aux.comment() != null ? aux.comment() : "");
    }

    @Override
    public void dispose() {
        EquationAutoComplete.detach(equationField);
    }

    private void commitComment(TextArea area) {
        String text = area.getText().trim();
        String comment = text.isEmpty() ? null : text;
        Optional<AuxDef> auxOpt = ctx.editor.getAuxByName(ctx.elementName);
        if (auxOpt.isEmpty() || Objects.equals(comment, auxOpt.get().comment())) {
            return;
        }
        ctx.canvas.applyMutation(() -> ctx.editor.setAuxComment(ctx.elementName, comment));
    }

    private void commitEquation(EquationField field) {
        String equation = field.getText().trim();
        if (equation.isEmpty()) {
            ctx.editor.getAuxByName(ctx.elementName)
                    .ifPresent(aux -> field.setText(aux.equation()));
            return;
        }
        Optional<AuxDef> auxOpt = ctx.editor.getAuxByName(ctx.elementName);
        if (auxOpt.isPresent() && equation.equals(auxOpt.get().equation())) {
            return;
        }
        ctx.canvas.applyMutation(() -> ctx.editor.setAuxEquation(ctx.elementName, equation));
    }

    private void commitUnit(ComboBox<String> box) {
        String unit = box.getValue() != null ? box.getValue().trim() : "";
        Optional<AuxDef> auxOpt = ctx.editor.getAuxByName(ctx.elementName);
        if (auxOpt.isPresent() && unit.equals(auxOpt.get().unit())) {
            return;
        }
        ctx.canvas.applyMutation(() -> ctx.editor.setAuxUnit(ctx.elementName, unit));
    }
}

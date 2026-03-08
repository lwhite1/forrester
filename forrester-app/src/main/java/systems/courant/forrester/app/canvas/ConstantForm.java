package systems.courant.forrester.app.canvas;

import systems.courant.forrester.model.def.ConstantDef;

import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

import java.util.Objects;
import java.util.Optional;

/**
 * Property form for constant elements. Builds editable fields
 * for name, value, and unit.
 */
class ConstantForm implements ElementForm {

    private final FormContext ctx;

    private TextField nameField;
    private TextField valueField;
    private ComboBox<String> unitBox;
    private TextArea commentArea;

    ConstantForm(FormContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public int build(int startRow) {
        Optional<ConstantDef> constantOpt = ctx.editor.getConstantByName(ctx.elementName);
        if (constantOpt.isEmpty()) {
            ctx.addReadOnlyRow(startRow++, "Name", ctx.elementName);
            return startRow;
        }
        ConstantDef constant = constantOpt.get();

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

        commentArea = new TextArea(constant.comment() != null ? constant.comment() : "");
        commentArea.setId("propComment");
        commentArea.setPrefRowCount(2);
        commentArea.setWrapText(true);
        commentArea.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(commentArea, Priority.ALWAYS);
        ctx.addTextAreaCommitHandlers(commentArea, this::commitComment);
        ctx.addFieldRow(row++, "Comment", commentArea,
                "Optional documentation for this element");

        return row;
    }

    @Override
    public void updateValues() {
        Optional<ConstantDef> constantOpt = ctx.editor.getConstantByName(ctx.elementName);
        if (constantOpt.isEmpty() || nameField == null) {
            return;
        }
        ConstantDef constant = constantOpt.get();
        nameField.setText(ctx.elementName);
        valueField.setText(ElementRenderer.formatValue(constant.value()));
        unitBox.setValue(constant.unit() != null ? constant.unit() : "");
        commentArea.setText(constant.comment() != null ? constant.comment() : "");
    }

    private void commitComment(TextArea area) {
        String text = area.getText().trim();
        String comment = text.isEmpty() ? null : text;
        Optional<ConstantDef> constantOpt = ctx.editor.getConstantByName(ctx.elementName);
        if (constantOpt.isEmpty() || Objects.equals(comment, constantOpt.get().comment())) {
            return;
        }
        ctx.canvas.applyMutation(() -> ctx.editor.setConstantComment(ctx.elementName, comment));
    }

    private void commitValue(TextField field) {
        try {
            double value = Double.parseDouble(field.getText().trim());
            Optional<ConstantDef> constantOpt = ctx.editor.getConstantByName(ctx.elementName);
            if (constantOpt.isEmpty() || constantOpt.get().value() == value) {
                return;
            }
            ctx.canvas.applyMutation(() -> ctx.editor.setConstantValue(ctx.elementName, value));
        } catch (NumberFormatException ignored) {
            ctx.editor.getConstantByName(ctx.elementName)
                    .ifPresent(constant -> field.setText(ElementRenderer.formatValue(constant.value())));
        }
    }

    private void commitUnit(ComboBox<String> box) {
        String unit = box.getValue() != null ? box.getValue().trim() : "";
        Optional<ConstantDef> constantOpt = ctx.editor.getConstantByName(ctx.elementName);
        if (constantOpt.isPresent() && unit.equals(constantOpt.get().unit())) {
            return;
        }
        ctx.canvas.applyMutation(() -> ctx.editor.setConstantUnit(ctx.elementName, unit));
    }
}

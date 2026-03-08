package systems.courant.forrester.app.canvas;

import systems.courant.forrester.model.def.CldVariableDef;

import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

import java.util.Objects;

/**
 * Property form for CLD variable elements. Builds editable fields
 * for name and comment.
 */
class CldVariableForm implements ElementForm {

    private final FormContext ctx;

    private TextField nameField;
    private TextArea commentArea;

    CldVariableForm(FormContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public int build(int startRow) {
        CldVariableDef variable = ctx.editor.getCldVariableByName(ctx.elementName);
        if (variable == null) {
            ctx.addReadOnlyRow(startRow++, "Name", ctx.elementName);
            return startRow;
        }

        int row = startRow;
        nameField = ctx.createNameField();
        ctx.addFieldRow(row++, "Name", nameField,
                "The name of this causal variable");

        commentArea = new TextArea(variable.comment() != null ? variable.comment() : "");
        commentArea.setId("propComment");
        commentArea.setPrefRowCount(2);
        commentArea.setWrapText(true);
        commentArea.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(commentArea, Priority.ALWAYS);
        ctx.addTextAreaCommitHandlers(commentArea, this::commitComment);
        ctx.addFieldRow(row++, "Comment", commentArea,
                "Optional description of this variable");

        return row;
    }

    @Override
    public void updateValues() {
        CldVariableDef variable = ctx.editor.getCldVariableByName(ctx.elementName);
        if (variable == null || nameField == null) {
            return;
        }
        nameField.setText(ctx.elementName);
        commentArea.setText(variable.comment() != null ? variable.comment() : "");
    }

    private void commitComment(TextArea area) {
        String text = area.getText().trim();
        String comment = text.isEmpty() ? null : text;
        CldVariableDef variable = ctx.editor.getCldVariableByName(ctx.elementName);
        if (variable == null || Objects.equals(comment, variable.comment())) {
            return;
        }
        ctx.canvas.applyMutation(() -> ctx.editor.setCldVariableComment(ctx.elementName, comment));
    }
}

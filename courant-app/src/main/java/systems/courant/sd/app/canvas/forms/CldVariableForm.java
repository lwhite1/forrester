package systems.courant.sd.app.canvas.forms;

import systems.courant.sd.model.def.CldVariableDef;

import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

import java.util.Objects;

/**
 * Property form for CLD variable elements. Builds editable fields
 * for name and comment.
 */
public class CldVariableForm implements ElementForm {

    private final FormContext ctx;

    private TextField nameField;
    private TextArea commentArea;

    public CldVariableForm(FormContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public int build(int startRow) {
        CldVariableDef variable = ctx.getEditor().getCldVariableByName(ctx.getElementName())
                .orElse(null);
        if (variable == null) {
            ctx.addReadOnlyRow(startRow++, "Name", ctx.getElementName());
            return startRow;
        }

        int row = startRow;
        nameField = ctx.createNameField();
        ctx.addFieldRow(row++, "Name", nameField,
                "The name of this causal variable");

        commentArea = ctx.addCommentArea(row++, variable.comment(), this::commitComment);

        return row;
    }

    @Override
    public void updateValues() {
        CldVariableDef variable = ctx.getEditor().getCldVariableByName(ctx.getElementName())
                .orElse(null);
        if (variable == null || nameField == null) {
            return;
        }
        nameField.setText(ctx.getElementName());
        commentArea.setText(variable.comment() != null ? variable.comment() : "");
    }

    private void commitComment(TextArea area) {
        String text = area.getText().trim();
        String comment = text.isEmpty() ? null : text;
        CldVariableDef variable = ctx.getEditor().getCldVariableByName(ctx.getElementName())
                .orElse(null);
        if (variable == null || Objects.equals(comment, variable.comment())) {
            return;
        }
        ctx.getCanvas().applyMutation(() -> ctx.getEditor().setCldVariableComment(ctx.getElementName(), comment));
    }
}

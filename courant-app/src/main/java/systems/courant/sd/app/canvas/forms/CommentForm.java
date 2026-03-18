package systems.courant.sd.app.canvas.forms;

import systems.courant.sd.model.def.CommentDef;

import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

import java.util.Objects;

/**
 * Property form for comment annotation elements. Builds an editable
 * text area for the comment content.
 */
public class CommentForm implements ElementForm {

    private final FormContext ctx;
    private final FormFieldBuilder fields;

    private TextArea textArea;

    public CommentForm(FormContext ctx, FormFieldBuilder fields) {
        this.ctx = ctx;
        this.fields = fields;
    }

    @Override
    public int build(int startRow) {
        CommentDef comment = ctx.getEditor().getCommentByName(ctx.getElementName());
        if (comment == null) {
            fields.addReadOnlyRow(startRow++, "Name", ctx.getElementName());
            return startRow;
        }

        int row = startRow;

        textArea = new TextArea(comment.text() != null ? comment.text() : "");
        textArea.setId("propCommentText");
        textArea.setPrefRowCount(4);
        textArea.setWrapText(true);
        textArea.setPromptText("Enter annotation text...");
        textArea.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(textArea, Priority.ALWAYS);
        fields.addTextAreaCommitHandlers(textArea, this::commitText);
        fields.addFieldRow(row++, "Text", textArea,
                "Free-text annotation displayed on the canvas");

        return row;
    }

    @Override
    public void updateValues() {
        CommentDef comment = ctx.getEditor().getCommentByName(ctx.getElementName());
        if (comment == null || textArea == null) {
            return;
        }
        textArea.setText(comment.text() != null ? comment.text() : "");
    }

    private void commitText(TextArea area) {
        String text = area.getText().trim();
        CommentDef comment = ctx.getEditor().getCommentByName(ctx.getElementName());
        if (comment == null || Objects.equals(text, comment.text())) {
            return;
        }
        ctx.getCanvas().applyMutation(() -> ctx.getEditor().setCommentText(ctx.getElementName(), text));
    }
}

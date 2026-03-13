package systems.courant.sd.app.canvas;

import systems.courant.sd.model.def.CommentDef;

import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

import java.util.Objects;

/**
 * Property form for comment annotation elements. Builds an editable
 * text area for the comment content.
 */
class CommentForm implements ElementForm {

    private final FormContext ctx;

    private TextArea textArea;

    CommentForm(FormContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public int build(int startRow) {
        CommentDef comment = ctx.editor.getCommentByName(ctx.elementName);
        if (comment == null) {
            ctx.addReadOnlyRow(startRow++, "Name", ctx.elementName);
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
        ctx.addTextAreaCommitHandlers(textArea, this::commitText);
        ctx.addFieldRow(row++, "Text", textArea,
                "Free-text annotation displayed on the canvas");

        return row;
    }

    @Override
    public void updateValues() {
        CommentDef comment = ctx.editor.getCommentByName(ctx.elementName);
        if (comment == null || textArea == null) {
            return;
        }
        textArea.setText(comment.text() != null ? comment.text() : "");
    }

    private void commitText(TextArea area) {
        String text = area.getText().trim();
        CommentDef comment = ctx.editor.getCommentByName(ctx.elementName);
        if (comment == null || Objects.equals(text, comment.text())) {
            return;
        }
        ctx.canvas.applyMutation(() -> ctx.editor.setCommentText(ctx.elementName, text));
    }
}

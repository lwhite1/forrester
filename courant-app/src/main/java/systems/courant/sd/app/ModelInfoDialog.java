package systems.courant.sd.app;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import systems.courant.sd.app.canvas.ModelEditor;
import systems.courant.sd.model.ModelMetadata;

/**
 * Dialog for viewing and editing model metadata: name, comment, author,
 * source, license, and URL.
 */
final class ModelInfoDialog {

    private ModelInfoDialog() {}

    /**
     * Shows the Model Info dialog. On OK, applies edits to the given
     * {@code editor} and invokes {@code onSaved}.
     */
    static void show(ModelEditor editor, Stage owner, Runnable onSaved) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Model Info");
        dialog.setHeaderText(null);
        dialog.initOwner(owner);

        dialog.getDialogPane().getButtonTypes().addAll(
                ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));

        TextField nameField = new TextField(editor.getModelName());
        nameField.setPrefColumnCount(30);
        TextArea commentArea = new TextArea(
                editor.getModelComment() != null
                        ? editor.getModelComment() : "");
        commentArea.setPrefRowCount(4);
        commentArea.setPrefColumnCount(30);

        ModelMetadata meta = editor.getMetadata();
        TextField authorField = new TextField(
                meta != null ? nullToEmpty(meta.author()) : "");
        authorField.setPrefColumnCount(30);
        TextField sourceField = new TextField(
                meta != null ? nullToEmpty(meta.source()) : "");
        sourceField.setPrefColumnCount(30);
        TextField licenseField = new TextField(
                meta != null ? nullToEmpty(meta.license()) : "");
        licenseField.setPrefColumnCount(30);
        TextField urlField = new TextField(
                meta != null ? nullToEmpty(meta.url()) : "");
        urlField.setPrefColumnCount(30);

        int row = 0;
        grid.add(new Label("Name:"), 0, row);
        grid.add(nameField, 1, row++);
        grid.add(new Label("Comment:"), 0, row);
        grid.add(commentArea, 1, row++);
        grid.add(new Label("Author:"), 0, row);
        grid.add(authorField, 1, row++);
        grid.add(new Label("Source:"), 0, row);
        grid.add(sourceField, 1, row++);
        grid.add(new Label("License:"), 0, row);
        grid.add(licenseField, 1, row++);
        grid.add(new Label("URL:"), 0, row);
        grid.add(urlField, 1, row);

        dialog.getDialogPane().setContent(grid);
        Platform.runLater(nameField::requestFocus);

        dialog.showAndWait()
                .filter(button -> button == ButtonType.OK)
                .ifPresent(button -> {
                    String newName = nameField.getText().trim();
                    editor.setModelName(
                            newName.isEmpty() ? "Untitled" : newName);
                    editor.setModelComment(commentArea.getText().trim());

                    String author = emptyToNull(authorField.getText());
                    String source = emptyToNull(sourceField.getText());
                    String license = emptyToNull(licenseField.getText());
                    String url = emptyToNull(urlField.getText());
                    if (author != null || source != null
                            || license != null || url != null) {
                        editor.setMetadata(ModelMetadata.builder()
                                .author(author).source(source)
                                .license(license).url(url)
                                .build());
                    } else {
                        editor.setMetadata(null);
                    }
                    onSaved.run();
                });
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static String emptyToNull(String s) {
        return s == null || s.trim().isEmpty() ? null : s.trim();
    }
}

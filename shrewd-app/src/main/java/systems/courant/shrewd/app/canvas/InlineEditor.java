package systems.courant.shrewd.app.canvas;

import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;

import java.util.function.Consumer;

/**
 * Manages a TextField or TextArea overlay for inline editing of element names,
 * values, equations, and multi-line text. The control is added to a parent Pane
 * (since Canvas cannot have children) and positioned at the element's screen
 * coordinates.
 *
 * <p>Enter commits the edit (for TextField), Escape cancels, and focus loss commits.</p>
 */
public class InlineEditor {

    private final Pane overlayPane;
    private TextField textField;
    private TextArea textArea;
    private Consumer<String> onCommit;

    public InlineEditor(Pane overlayPane) {
        this.overlayPane = overlayPane;
    }

    /**
     * Returns true if an editing control is currently open.
     */
    public boolean isActive() {
        return textField != null || textArea != null;
    }

    /**
     * Returns the current TextField, or null if no single-line edit is active.
     */
    public TextField getTextField() {
        return textField;
    }

    /**
     * Opens a TextField at the given screen coordinates for inline editing.
     *
     * @param screenX    X position in the overlay pane's coordinate space
     * @param screenY    Y position in the overlay pane's coordinate space
     * @param initialText the initial text to display (selected)
     * @param fieldWidth  the width of the TextField
     * @param onCommit   callback invoked with the final text on Enter or focus loss
     */
    public void open(double screenX, double screenY, String initialText,
                     double fieldWidth, Consumer<String> onCommit) {
        close();
        this.onCommit = onCommit;

        textField = new TextField(initialText);
        textField.setPrefWidth(fieldWidth);
        textField.setLayoutX(screenX - fieldWidth / 2);
        textField.setLayoutY(screenY - textField.prefHeight(-1) / 2);

        textField.setOnAction(event -> commit());

        textField.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case ESCAPE -> {
                    this.onCommit = null;
                    close();
                    event.consume();
                }
                default -> { }
            }
        });

        textField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused && textField != null) {
                commit();
            }
        });

        overlayPane.getChildren().add(textField);
        textField.selectAll();
        textField.requestFocus();
    }

    /**
     * Opens a TextArea at the given screen coordinates for multi-line inline editing.
     *
     * @param screenX     center X position in the overlay pane's coordinate space
     * @param screenY     center Y position in the overlay pane's coordinate space
     * @param initialText the initial text to display
     * @param width       the width of the TextArea
     * @param height      the height of the TextArea
     * @param onCommit    callback invoked with the final text on Escape or focus loss
     */
    public void openTextArea(double screenX, double screenY, String initialText,
                             double width, double height, Consumer<String> onCommit) {
        close();
        this.onCommit = onCommit;

        textArea = new TextArea(initialText);
        textArea.setPrefWidth(width);
        textArea.setPrefHeight(height);
        textArea.setWrapText(true);
        textArea.setLayoutX(screenX - width / 2);
        textArea.setLayoutY(screenY - height / 2);

        textArea.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case ESCAPE -> {
                    commitTextArea();
                    event.consume();
                }
                default -> { }
            }
        });

        textArea.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused && textArea != null) {
                commitTextArea();
            }
        });

        overlayPane.getChildren().add(textArea);
        textArea.positionCaret(initialText.length());
        textArea.requestFocus();
    }

    /**
     * Closes the inline editor, removing any active control from the overlay pane.
     */
    public void close() {
        if (textField != null) {
            EquationAutoComplete.detach(textField);
            overlayPane.getChildren().remove(textField);
            textField = null;
        }
        if (textArea != null) {
            overlayPane.getChildren().remove(textArea);
            textArea = null;
        }
        onCommit = null;
    }

    private void commit() {
        if (textField == null) {
            return;
        }
        String text = textField.getText();
        Consumer<String> callback = onCommit;
        onCommit = null;
        close();
        if (callback != null) {
            callback.accept(text);
        }
    }

    private void commitTextArea() {
        if (textArea == null) {
            return;
        }
        String text = textArea.getText();
        Consumer<String> callback = onCommit;
        onCommit = null;
        close();
        if (callback != null) {
            callback.accept(text);
        }
    }
}

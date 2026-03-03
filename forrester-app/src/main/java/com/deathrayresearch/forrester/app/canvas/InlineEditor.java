package com.deathrayresearch.forrester.app.canvas;

import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;

import java.util.function.Consumer;

/**
 * Manages a TextField overlay for inline editing of element names and values.
 * The TextField is added to a parent Pane (since Canvas cannot have children)
 * and positioned at the element's screen coordinates.
 *
 * <p>Enter commits the edit, Escape cancels, and focus loss commits.</p>
 */
public class InlineEditor {

    private final Pane overlayPane;
    private TextField textField;
    private Consumer<String> onCommit;

    public InlineEditor(Pane overlayPane) {
        this.overlayPane = overlayPane;
    }

    /**
     * Returns true if a TextField is currently open.
     */
    public boolean isActive() {
        return textField != null;
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
     * Closes the inline editor, removing the TextField from the overlay pane.
     */
    public void close() {
        if (textField != null) {
            overlayPane.getChildren().remove(textField);
            textField = null;
            onCommit = null;
        }
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
}

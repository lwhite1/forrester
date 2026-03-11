package systems.courant.shrewd.app.canvas;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;

/**
 * Abstraction over text input controls used for equation editing.
 * Implemented by {@link TextFieldEquationField} (single-line, used for inline editing)
 * and {@link CodeAreaEquationField} (multi-line with syntax highlighting, used in property forms).
 */
interface EquationField {

    /** Returns the current text content. */
    String getText();

    /** Replaces the entire text content. */
    void setText(String text);

    /** Selects all text. */
    void selectAll();

    /** Returns the current caret (cursor) position as a character offset. */
    int getCaretPosition();

    /** Moves the caret to the given character offset. */
    void positionCaret(int position);

    /** Requests keyboard focus for this control. */
    void requestFocus();

    /** Observable text content for attaching change listeners. */
    ObservableValue<String> textObservable();

    /** Observable caret position for attaching change listeners. */
    ObservableValue<Number> caretPositionObservable();

    /** Observable focus state. */
    ReadOnlyBooleanProperty focusedProperty();

    /** Returns the underlying JavaFX {@link Node} for layout, event filters, and properties. */
    Node node();

    /** Sets an inline CSS style on the control (e.g., error border). */
    void setFieldStyle(String style);

    /** Sets the action handler invoked when the user presses Enter to commit. */
    void setOnAction(EventHandler<ActionEvent> handler);

    /** Returns the current action handler. */
    EventHandler<ActionEvent> getOnAction();
}

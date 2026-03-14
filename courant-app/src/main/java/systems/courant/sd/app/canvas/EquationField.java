package systems.courant.sd.app.canvas;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import systems.courant.sd.app.canvas.forms.CodeAreaEquationField;
import systems.courant.sd.app.canvas.forms.TextFieldEquationField;

/**
 * Abstraction over text input controls used for equation editing.
 * Implemented by {@link TextFieldEquationField} (single-line, used for inline editing)
 * and {@link CodeAreaEquationField} (multi-line with syntax highlighting, used in property forms).
 */
public interface EquationField {

    /** Returns the current text content. */
    public String getText();

    /** Replaces the entire text content. */
    public void setText(String text);

    /** Selects all text. */
    public void selectAll();

    /** Returns the current caret (cursor) position as a character offset. */
    public int getCaretPosition();

    /** Moves the caret to the given character offset. */
    public void positionCaret(int position);

    /** Requests keyboard focus for this control. */
    public void requestFocus();

    /** Observable text content for attaching change listeners. */
    public ObservableValue<String> textObservable();

    /** Observable caret position for attaching change listeners. */
    public ObservableValue<Number> caretPositionObservable();

    /** Observable focus state. */
    public ReadOnlyBooleanProperty focusedProperty();

    /** Returns the underlying JavaFX {@link Node} for layout, event filters, and properties. */
    public Node node();

    /** Sets an inline CSS style on the control (e.g., error border). */
    public void setFieldStyle(String style);

    /** Sets the action handler invoked when the user presses Enter to commit. */
    public void setOnAction(EventHandler<ActionEvent> handler);

    /** Returns the current action handler. */
    public EventHandler<ActionEvent> getOnAction();
}

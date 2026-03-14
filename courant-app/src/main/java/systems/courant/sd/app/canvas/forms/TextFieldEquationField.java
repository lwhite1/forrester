package systems.courant.sd.app.canvas.forms;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import systems.courant.sd.app.canvas.EquationField;

/**
 * Adapts a standard JavaFX {@link TextField} to the {@link EquationField} interface.
 * Used for inline equation editing on the canvas where a single-line field is appropriate.
 */
public final class TextFieldEquationField implements EquationField {

    private final TextField textField;

    public TextFieldEquationField(TextField textField) {
        this.textField = textField;
    }

    @Override
    public String getText() {
        return textField.getText();
    }

    @Override
    public void setText(String text) {
        textField.setText(text);
    }

    @Override
    public void selectAll() {
        textField.selectAll();
    }

    @Override
    public int getCaretPosition() {
        return textField.getCaretPosition();
    }

    @Override
    public void positionCaret(int position) {
        textField.positionCaret(position);
    }

    @Override
    public void requestFocus() {
        textField.requestFocus();
    }

    @Override
    public ObservableValue<String> textObservable() {
        return textField.textProperty();
    }

    @Override
    public ObservableValue<Number> caretPositionObservable() {
        return textField.caretPositionProperty();
    }

    @Override
    public ReadOnlyBooleanProperty focusedProperty() {
        return textField.focusedProperty();
    }

    @Override
    public Node node() {
        return textField;
    }

    @Override
    public void setFieldStyle(String style) {
        textField.setStyle(style);
    }

    @Override
    public void setOnAction(EventHandler<ActionEvent> handler) {
        textField.setOnAction(handler);
    }

    @Override
    public EventHandler<ActionEvent> getOnAction() {
        return textField.getOnAction();
    }
}

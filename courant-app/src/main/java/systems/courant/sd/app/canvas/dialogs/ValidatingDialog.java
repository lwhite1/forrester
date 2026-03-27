package systems.courant.sd.app.canvas.dialogs;

import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;

import systems.courant.sd.app.canvas.HelpContextResolver;
import systems.courant.sd.app.canvas.Styles;

import java.util.concurrent.Callable;

/**
 * Base class for configuration dialogs with OK/Cancel buttons and optional validation.
 *
 * <p>Eliminates boilerplate shared by 10+ dialog subclasses: button creation,
 * result converter wiring, help button, dialog width, and validation label/binding.
 *
 * <p>Subclasses override {@link #buildResult()} to construct the result object,
 * and optionally call {@link #bindValidation} or {@link #bindOkDisable} for validation.
 *
 * @param <R> the result type returned when the user presses OK
 */
public abstract class ValidatingDialog<R> extends Dialog<R> {

    private final ButtonType okButton;

    protected ValidatingDialog(String title, String headerText) {
        okButton = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(okButton, ButtonType.CANCEL);

        HelpContextResolver.addHelpButton(this);
        setTitle(title);
        setHeaderText(headerText);

        setResultConverter(button -> {
            if (button == okButton) {
                return buildResult();
            }
            return null;
        });
    }

    /**
     * Constructs the result object from the dialog's current field values.
     * Called when the user presses OK.
     */
    protected abstract R buildResult();

    /**
     * Sets the dialog content and applies the standard configuration dialog width.
     */
    protected void setStandardContent(Node content) {
        getDialogPane().setContent(content);
        getDialogPane().setPrefWidth(Styles.screenAwareWidth(Styles.CONFIG_DIALOG_WIDTH));
    }

    /**
     * Binds the OK button's disabled state to an observable boolean condition.
     *
     * <p>Use this when validation is a simple boolean (no message label needed).
     */
    protected void bindOkDisable(ObservableValue<Boolean> disableCondition) {
        getDialogPane().lookupButton(okButton).disableProperty().bind(disableCondition);
    }

    /**
     * Creates a validation label bound to a message supplier and binds the OK button
     * to be disabled when the message is non-empty.
     *
     * <p>The returned label should be added to the dialog's layout by the subclass.
     *
     * @param id           CSS id for the label
     * @param message      supplier that returns an empty string when valid, or an error message
     * @param dependencies observables that trigger re-evaluation of the message
     * @return the bound validation label
     */
    protected Label bindValidation(String id, Callable<String> message,
                                   Observable... dependencies) {
        Label label = new Label();
        label.setStyle(Styles.VALIDATION_ERROR);
        label.setWrapText(true);
        label.setMaxWidth(Double.MAX_VALUE);
        label.setId(id);
        label.textProperty().bind(Bindings.createStringBinding(message, dependencies));

        getDialogPane().lookupButton(okButton).disableProperty().bind(
                Bindings.createBooleanBinding(() -> {
                    try {
                        return !message.call().isEmpty();
                    } catch (Exception e) {
                        return true;
                    }
                }, dependencies));

        return label;
    }
}

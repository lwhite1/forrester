package systems.courant.sd.app.canvas;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

import java.util.List;

/**
 * Base class for parameter row components used in sweep, Monte Carlo, and
 * optimizer dialogs. Provides the shared infrastructure: a name ComboBox,
 * an HBox pane, a remove button, and field-change notification.
 *
 * <p>Subclasses add dialog-specific fields and implement {@link #isValid()}.
 */
abstract class ParameterRowBase {

    protected final ComboBox<String> nameCombo;
    protected final HBox pane;
    private final Runnable onChange;

    /**
     * Creates a parameter row with a name ComboBox pre-populated from the given constant names.
     *
     * @param constantNames available parameter names
     * @param defaultName   initial selection (null to use first available)
     * @param onChange       callback invoked when any field changes
     */
    protected ParameterRowBase(List<String> constantNames, String defaultName, Runnable onChange) {
        this.onChange = onChange;

        nameCombo = new ComboBox<>(FXCollections.observableArrayList(constantNames));
        if (defaultName != null) {
            nameCombo.setValue(defaultName);
        } else if (!constantNames.isEmpty()) {
            nameCombo.setValue(constantNames.getFirst());
        }
        nameCombo.setPrefWidth(130);

        pane = new HBox(6);
        pane.setPadding(new Insets(2));
    }

    /**
     * Creates a remove button that calls the given callback when clicked.
     */
    protected Button createRemoveButton(Runnable onRemove) {
        Button removeBtn = new Button("X");
        removeBtn.setOnAction(e -> onRemove.run());
        return removeBtn;
    }

    /**
     * Registers a text-change listener on a field that triggers the onChange callback.
     */
    protected void wireFieldChange(TextField field) {
        field.textProperty().addListener((obs, o, n) -> {
            if (onChange != null) {
                onChange.run();
            }
        });
    }

    /**
     * Populates the pane with the name combo, the given custom nodes, and the remove button.
     */
    protected void buildPane(Button removeBtn, Node... customFields) {
        pane.getChildren().add(nameCombo);
        pane.getChildren().addAll(customFields);
        pane.getChildren().add(removeBtn);
    }

    HBox getPane() {
        return pane;
    }

    /**
     * Returns true if this row has a valid, complete configuration.
     */
    abstract boolean isValid();

    /**
     * Returns the selected parameter name, or null if none is selected.
     */
    String getSelectedName() {
        return nameCombo.getValue();
    }

    /**
     * Returns true if a parameter name is selected.
     */
    boolean isNameSelected() {
        return nameCombo.getValue() != null;
    }
}

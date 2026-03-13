package systems.courant.sd.app.canvas;

import systems.courant.sd.model.def.ModuleInstanceDef;
import systems.courant.sd.model.def.ModuleInterface;
import systems.courant.sd.model.def.PortDef;

import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dialog for configuring module input and output port bindings.
 * Shows a grid of port name labels with text fields for binding expressions
 * (inputs) or alias names (outputs).
 */
public class BindingConfigDialog extends Dialog<BindingConfigDialog.BindingResult> {

    /**
     * Result containing the configured input and output bindings.
     */
    public record BindingResult(
            Map<String, String> inputBindings,
            Map<String, String> outputBindings
    ) {}

    private final Map<String, TextField> inputFields = new HashMap<>();
    private final Map<String, TextField> outputFields = new HashMap<>();

    public BindingConfigDialog(ModuleInstanceDef module) {
        setTitle("Configure Bindings — " + module.instanceName());
        setHeaderText("Configure port bindings for module '" + module.instanceName() + "'");

        ModuleInterface iface = module.definition().moduleInterface();

        VBox content = new VBox(10);
        content.setPadding(new Insets(10));

        // Input bindings section
        Label inputHeader = new Label("Input Bindings");
        inputHeader.setStyle(Styles.SECTION_HEADER);
        content.getChildren().add(inputHeader);

        if (iface != null && !iface.inputs().isEmpty()) {
            GridPane inputGrid = createPortGrid(iface.inputs(),
                    module.inputBindings(), inputFields, "Expression");
            content.getChildren().add(inputGrid);
        } else {
            Label noInputs = new Label("(No input ports defined)");
            noInputs.setStyle(Styles.MUTED_TEXT);
            content.getChildren().add(noInputs);
        }

        // Output bindings section
        Label outputHeader = new Label("Output Bindings");
        outputHeader.setStyle(Styles.SECTION_HEADER);
        content.getChildren().add(outputHeader);

        if (iface != null && !iface.outputs().isEmpty()) {
            GridPane outputGrid = createPortGrid(iface.outputs(),
                    module.outputBindings(), outputFields, "Alias");
            content.getChildren().add(outputGrid);
        } else {
            Label noOutputs = new Label("(No output ports defined)");
            noOutputs.setStyle(Styles.MUTED_TEXT);
            content.getChildren().add(noOutputs);
        }

        getDialogPane().setContent(content);

        ButtonType okButton = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(okButton, ButtonType.CANCEL);

        setResultConverter(button -> {
            if (button == okButton) {
                return new BindingResult(
                        collectBindings(inputFields),
                        collectBindings(outputFields));
            }
            return null;
        });
    }

    private GridPane createPortGrid(List<PortDef> ports, Map<String, String> existingBindings,
                                     Map<String, TextField> fieldMap, String fieldLabel) {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(6);
        grid.setPadding(new Insets(4, 0, 8, 10));

        grid.add(new Label("Port"), 0, 0);
        Label headerLabel = new Label(fieldLabel);
        headerLabel.setStyle(Styles.BOLD_TEXT);
        grid.add(headerLabel, 1, 0);

        int row = 1;
        for (PortDef port : ports) {
            Label nameLabel = new Label(port.name());
            if (port.unit() != null && !port.unit().isBlank()) {
                nameLabel.setText(port.name() + " (" + port.unit() + ")");
            }
            grid.add(nameLabel, 0, row);

            TextField field = new TextField();
            field.setPrefWidth(200);
            String existing = existingBindings.get(port.name());
            if (existing != null) {
                field.setText(existing);
            }
            grid.add(field, 1, row);
            fieldMap.put(port.name(), field);

            row++;
        }

        return grid;
    }

    private Map<String, String> collectBindings(Map<String, TextField> fields) {
        Map<String, String> bindings = new HashMap<>();
        for (Map.Entry<String, TextField> entry : fields.entrySet()) {
            String value = entry.getValue().getText();
            if (value != null && !value.isBlank()) {
                bindings.put(entry.getKey(), value.trim());
            }
        }
        return bindings;
    }
}

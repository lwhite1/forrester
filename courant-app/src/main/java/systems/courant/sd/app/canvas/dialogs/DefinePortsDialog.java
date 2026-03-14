package systems.courant.sd.app.canvas.dialogs;

import systems.courant.sd.model.def.ModuleInterface;
import systems.courant.sd.model.def.PortDef;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import systems.courant.sd.app.canvas.Styles;

/**
 * Dialog for defining a module's input and output ports.
 * Each port has a name and an optional unit. Ports can be added and removed.
 */
public class DefinePortsDialog extends Dialog<ModuleInterface> {

    private final VBox inputPortsBox = new VBox(4);
    private final VBox outputPortsBox = new VBox(4);
    private final List<PortRow> inputRows = new ArrayList<>();
    private final List<PortRow> outputRows = new ArrayList<>();

    private record PortRow(TextField nameField, TextField unitField, HBox container) {}

    public DefinePortsDialog(String moduleName, ModuleInterface existing) {
        setTitle("Define Ports — " + moduleName);
        setHeaderText("Define input and output ports for module '" + moduleName + "'");
        setResizable(true);

        VBox content = new VBox(12);
        content.setPadding(new Insets(10));
        content.setPrefWidth(420);

        // Input ports section
        Label inputHeader = new Label("Input Ports");
        inputHeader.setStyle(Styles.SECTION_HEADER);

        Label inputHelp = new Label("Input ports receive values from the parent model.");
        inputHelp.setStyle(Styles.MUTED_TEXT);

        GridPane inputColumnHeaders = createColumnHeaders();

        Button addInputButton = new Button("+ Add Input Port");
        addInputButton.setOnAction(e -> addPortRow(inputRows, inputPortsBox, "", ""));

        content.getChildren().addAll(inputHeader, inputHelp, inputColumnHeaders,
                inputPortsBox, addInputButton);

        // Output ports section
        Label outputHeader = new Label("Output Ports");
        outputHeader.setStyle(Styles.SECTION_HEADER);

        Label outputHelp = new Label("Output ports expose values to the parent model.");
        outputHelp.setStyle(Styles.MUTED_TEXT);

        GridPane outputColumnHeaders = createColumnHeaders();

        Button addOutputButton = new Button("+ Add Output Port");
        addOutputButton.setOnAction(e -> addPortRow(outputRows, outputPortsBox, "", ""));

        content.getChildren().addAll(outputHeader, outputHelp, outputColumnHeaders,
                outputPortsBox, addOutputButton);

        // Populate existing ports
        if (existing != null) {
            for (PortDef port : existing.inputs()) {
                addPortRow(inputRows, inputPortsBox,
                        port.name(), port.unit() != null ? port.unit() : "");
            }
            for (PortDef port : existing.outputs()) {
                addPortRow(outputRows, outputPortsBox,
                        port.name(), port.unit() != null ? port.unit() : "");
            }
        }

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setMaxHeight(450);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        getDialogPane().setContent(scrollPane);
        getDialogPane().setPrefHeight(550);

        ButtonType okButton = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(okButton, ButtonType.CANCEL);

        setResultConverter(button -> {
            if (button == okButton) {
                List<PortDef> inputs = collectPorts(inputRows);
                List<PortDef> outputs = collectPorts(outputRows);
                return new ModuleInterface(inputs, outputs);
            }
            return null;
        });
    }

    private GridPane createColumnHeaders() {
        GridPane headers = new GridPane();
        headers.setHgap(10);
        headers.setPadding(new Insets(0, 0, 0, 0));

        Label nameHeader = new Label("Name");
        nameHeader.setStyle(Styles.BOLD_TEXT);
        nameHeader.setPrefWidth(180);
        headers.add(nameHeader, 0, 0);

        Label unitHeader = new Label("Unit");
        unitHeader.setStyle(Styles.BOLD_TEXT);
        unitHeader.setPrefWidth(120);
        headers.add(unitHeader, 1, 0);

        return headers;
    }

    private void addPortRow(List<PortRow> rows, VBox container,
                            String name, String unit) {
        TextField nameField = new TextField(name);
        nameField.setPrefWidth(180);
        nameField.setPromptText("Port name");

        TextField unitField = new TextField(unit);
        unitField.setPrefWidth(120);
        unitField.setPromptText("Unit (optional)");

        Button removeButton = new Button("\u2715");
        removeButton.setStyle("-fx-font-size: 10px; -fx-padding: 2 6 2 6;");

        HBox row = new HBox(10, nameField, unitField, removeButton);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        PortRow portRow = new PortRow(nameField, unitField, row);
        rows.add(portRow);
        container.getChildren().add(row);

        removeButton.setOnAction(e -> {
            rows.remove(portRow);
            container.getChildren().remove(row);
        });
    }

    private List<PortDef> collectPorts(List<PortRow> rows) {
        List<PortDef> ports = new ArrayList<>();
        for (PortRow row : rows) {
            String name = row.nameField().getText();
            if (name != null && !name.isBlank()) {
                String unit = row.unitField().getText();
                if (unit != null && unit.isBlank()) {
                    unit = null;
                }
                ports.add(new PortDef(name.trim(), unit != null ? unit.trim() : null));
            }
        }
        return ports;
    }
}

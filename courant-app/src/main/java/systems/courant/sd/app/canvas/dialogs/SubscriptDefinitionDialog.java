package systems.courant.sd.app.canvas.dialogs;

import systems.courant.sd.app.canvas.HelpContextResolver;
import systems.courant.sd.app.canvas.Styles;
import systems.courant.sd.model.def.SubscriptDef;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Dialog for defining subscript dimensions (name + ordered label list).
 * Each dimension row has a name field, a comma-separated labels field,
 * and a remove button. New dimensions can be added dynamically.
 */
public class SubscriptDefinitionDialog extends Dialog<List<SubscriptDef>> {

    private final VBox rowsBox = new VBox(6);
    private final List<DimensionRow> rows = new ArrayList<>();

    private record DimensionRow(TextField nameField, TextField labelsField, HBox container) {}

    public SubscriptDefinitionDialog(List<SubscriptDef> existing) {
        HelpContextResolver.addHelpButton(this);
        setTitle("Subscript Dimensions");
        setHeaderText("Define subscript dimensions for arrayed elements");
        setResizable(true);

        VBox content = new VBox(12);
        content.setPadding(new Insets(10));
        content.setPrefWidth(480);

        Label help = new Label(
                "Each dimension has a name and a comma-separated list of labels. "
                + "Assign dimensions to elements in the properties panel.");
        help.setStyle(Styles.MUTED_TEXT);
        help.setWrapText(true);

        GridPane columnHeaders = createColumnHeaders();

        Button addButton = new Button("+ Add Dimension");
        addButton.setId("addDimensionButton");
        addButton.setOnAction(e -> addRow("", ""));

        content.getChildren().addAll(help, columnHeaders, rowsBox, addButton);

        if (existing != null) {
            for (SubscriptDef def : existing) {
                addRow(def.name(), String.join(", ", def.labels()));
            }
        }

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setMaxHeight(400);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        getDialogPane().setContent(scrollPane);
        getDialogPane().setPrefHeight(350);

        ButtonType okButton = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(okButton, ButtonType.CANCEL);

        setResultConverter(button -> {
            if (button == okButton) {
                return collectDimensions();
            }
            return null;
        });
    }

    private GridPane createColumnHeaders() {
        GridPane headers = new GridPane();
        headers.setHgap(10);

        Label nameHeader = new Label("Name");
        nameHeader.setStyle(Styles.BOLD_TEXT);
        nameHeader.setPrefWidth(140);
        headers.add(nameHeader, 0, 0);

        Label labelsHeader = new Label("Labels");
        labelsHeader.setStyle(Styles.BOLD_TEXT);
        labelsHeader.setPrefWidth(260);
        headers.add(labelsHeader, 1, 0);

        return headers;
    }

    private void addRow(String name, String labels) {
        TextField nameField = new TextField(name);
        nameField.setPrefWidth(140);
        nameField.setPromptText("e.g., Region");

        TextField labelsField = new TextField(labels);
        labelsField.setPrefWidth(260);
        labelsField.setPromptText("e.g., North, South, East, West");
        Tooltip.install(labelsField, new Tooltip("Comma-separated list of labels"));
        HBox.setHgrow(labelsField, Priority.ALWAYS);

        Button removeButton = new Button("\u2715");
        removeButton.setStyle("-fx-font-size: 10px; -fx-padding: 2 6 2 6;");

        HBox row = new HBox(10, nameField, labelsField, removeButton);
        row.setAlignment(Pos.CENTER_LEFT);

        DimensionRow dimRow = new DimensionRow(nameField, labelsField, row);
        rows.add(dimRow);
        rowsBox.getChildren().add(row);

        removeButton.setOnAction(e -> {
            rows.remove(dimRow);
            rowsBox.getChildren().remove(row);
        });
    }

    private List<SubscriptDef> collectDimensions() {
        List<SubscriptDef> result = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (DimensionRow row : rows) {
            String name = row.nameField().getText();
            if (name == null || name.isBlank()) {
                continue;
            }
            String trimmedName = name.trim();
            if (!seen.add(trimmedName)) {
                continue; // skip duplicate names
            }
            String labelsText = row.labelsField().getText();
            if (labelsText == null || labelsText.isBlank()) {
                continue;
            }
            List<String> labels = Arrays.stream(labelsText.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
            if (labels.isEmpty()) {
                continue;
            }
            result.add(new SubscriptDef(trimmedName, labels));
        }
        return result;
    }
}

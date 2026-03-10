package systems.courant.forrester.app.canvas;

import systems.courant.forrester.model.def.ValidationIssue;
import systems.courant.forrester.model.def.ValidationResult;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

import java.util.function.Consumer;

/**
 * A separate window that displays model validation results in a table.
 * Clicking a row with an element name invokes a callback to select it on the canvas.
 */
public class ValidationDialog extends Stage {

    public ValidationDialog(ValidationResult result, Consumer<String> onSelectElement) {
        setTitle("Model Validation");

        TableView<ValidationIssue> table = new TableView<>();
        table.setPlaceholder(new Label("No issues found. Model is clean."));

        TableColumn<ValidationIssue, String> severityCol = new TableColumn<>("Severity");
        severityCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().severity().name()));
        severityCol.setPrefWidth(80);

        TableColumn<ValidationIssue, String> elementCol = new TableColumn<>("Element");
        elementCol.setCellValueFactory(data -> {
            String name = data.getValue().elementName();
            return new SimpleStringProperty(name != null ? name : "");
        });
        elementCol.setPrefWidth(150);

        TableColumn<ValidationIssue, String> messageCol = new TableColumn<>("Message");
        messageCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().message()));
        messageCol.setPrefWidth(460);

        table.getColumns().add(severityCol);
        table.getColumns().add(elementCol);
        table.getColumns().add(messageCol);

        table.setItems(FXCollections.observableArrayList(result.issues()));

        // Click a row to select the element on the canvas
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.elementName() != null && onSelectElement != null) {
                onSelectElement.accept(newVal.elementName());
            }
        });

        // Summary label
        Label summaryLabel = new Label();
        summaryLabel.setPadding(new Insets(6, 8, 6, 8));
        if (result.isClean()) {
            summaryLabel.setText("No issues found");
        } else {
            summaryLabel.setText(result.errorCount() + " errors, "
                    + result.warningCount() + " warnings");
        }

        Button copyButton = new Button("Copy to Clipboard");
        copyButton.setOnAction(e -> {
            String text = formatAsText(result);
            ClipboardContent content = new ClipboardContent();
            content.putString(text);
            Clipboard.getSystemClipboard().setContent(content);
            copyButton.setText("Copied!");
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox bottomBar = new HBox(summaryLabel, spacer, copyButton);
        bottomBar.setAlignment(Pos.CENTER_LEFT);
        bottomBar.setPadding(new Insets(4, 8, 4, 0));

        BorderPane root = new BorderPane();
        root.setCenter(table);
        root.setBottom(bottomBar);

        Scene scene = new Scene(root, 700, 400);
        setScene(scene);
    }

    private static String formatAsText(ValidationResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("Model Validation Results\n");
        sb.append("=======================\n\n");
        if (result.isClean()) {
            sb.append("No issues found. Model is clean.\n");
        } else {
            sb.append(result.errorCount()).append(" errors, ")
                    .append(result.warningCount()).append(" warnings\n\n");
            for (ValidationIssue issue : result.issues()) {
                sb.append("[").append(issue.severity()).append("] ");
                if (issue.elementName() != null) {
                    sb.append(issue.elementName()).append(": ");
                }
                sb.append(issue.message()).append("\n");
            }
        }
        return sb.toString();
    }
}

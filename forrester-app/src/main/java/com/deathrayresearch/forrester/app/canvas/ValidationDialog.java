package com.deathrayresearch.forrester.app.canvas;

import com.deathrayresearch.forrester.model.def.ValidationIssue;
import com.deathrayresearch.forrester.model.def.ValidationResult;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
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

        BorderPane root = new BorderPane();
        root.setCenter(table);
        root.setBottom(summaryLabel);

        Scene scene = new Scene(root, 700, 400);
        setScene(scene);
    }
}

package systems.courant.sd.app.canvas.dialogs;

import systems.courant.sd.model.def.ValidationIssue;
import systems.courant.sd.model.def.ValidationResult;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.function.Consumer;
import systems.courant.sd.app.canvas.Styles;

/**
 * A dialog that displays model validation results in a table.
 * Clicking a row with an element name invokes a callback to select it on the canvas.
 *
 * <p>Only one validation dialog may be open at a time. Use {@link #showOrUpdate} to
 * create a new dialog or refresh and bring an existing one to the front.
 */
public class ValidationDialog extends Dialog<Void> {

    private static ValidationDialog openInstance;

    private final TableView<ValidationIssue> table;
    private final Label summaryLabel;
    private final Button copyButton;
    private ValidationResult currentResult;
    private Consumer<String> onSelectElement;

    /**
     * Shows a validation dialog with the given result. If a dialog is already open,
     * refreshes its contents and brings it to the front instead of creating a new one.
     *
     * @param result          the validation result to display
     * @param onSelectElement callback invoked when a row is clicked
     */
    public static void showOrUpdate(ValidationResult result, Consumer<String> onSelectElement) {
        if (openInstance != null && openInstance.isShowing()) {
            openInstance.updateResult(result);
            openInstance.onSelectElement = onSelectElement;
            Stage window = (Stage) openInstance.getDialogPane().getScene().getWindow();
            window.toFront();
            window.requestFocus();
            return;
        }
        ValidationDialog dialog = new ValidationDialog(result, onSelectElement);
        openInstance = dialog;
        dialog.show();
    }

    /**
     * Returns the currently open validation dialog, or {@code null} if none is showing.
     * Visible for testing.
     */
    public static ValidationDialog getOpenInstance() {
        if (openInstance != null && !openInstance.isShowing()) {
            openInstance = null;
        }
        return openInstance;
    }

    public ValidationDialog(ValidationResult result, Consumer<String> onSelectElement) {
        initModality(Modality.NONE);
        setTitle("Model Validation");
        this.currentResult = result;
        this.onSelectElement = onSelectElement;

        table = new TableView<>();
        table.setId("validationTable");
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
            if (newVal != null && newVal.elementName() != null && this.onSelectElement != null) {
                this.onSelectElement.accept(newVal.elementName());
            }
        });

        // Summary label
        summaryLabel = new Label();
        summaryLabel.setId("validationSummary");
        summaryLabel.setPadding(new Insets(6, 8, 6, 8));
        updateSummaryLabel(result);

        copyButton = new Button("Copy to Clipboard");
        copyButton.setId("validationCopy");
        copyButton.setOnAction(e -> {
            String text = formatAsText(currentResult);
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

        getDialogPane().setContent(root);
        getDialogPane().setPrefWidth(Styles.screenAwareWidth(700));
        getDialogPane().setPrefHeight(Styles.screenAwareHeight(400));
        getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        setResultConverter(button -> null);

        setOnHidden(e -> {
            if (openInstance == this) {
                openInstance = null;
            }
        });
    }

    /**
     * Replaces the displayed validation result with a new one.
     *
     * @param result the new validation result
     */
    public void updateResult(ValidationResult result) {
        this.currentResult = result;
        table.setItems(FXCollections.observableArrayList(result.issues()));
        updateSummaryLabel(result);
        copyButton.setText("Copy to Clipboard");
    }

    private void updateSummaryLabel(ValidationResult result) {
        if (result.isClean()) {
            summaryLabel.setText("No issues found");
        } else {
            summaryLabel.setText(result.errorCount() + " errors, "
                    + result.warningCount() + " warnings");
        }
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

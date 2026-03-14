package systems.courant.sd.app.canvas;

import systems.courant.sd.sweep.ExtremeConditionFinding;
import systems.courant.sd.sweep.ExtremeConditionResult;

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

/**
 * A dialog that displays extreme-condition test results in a table.
 *
 * <p>Only one instance may be open at a time. Use {@link #showOrUpdate} to
 * create a new dialog or refresh and bring an existing one to the front.
 */
public class ExtremeConditionDialog extends Dialog<Void> {

    private static ExtremeConditionDialog openInstance;

    private final TableView<ExtremeConditionFinding> table;
    private final Label summaryLabel;
    private final Button copyButton;
    private ExtremeConditionResult currentResult;

    /**
     * Shows an extreme-condition dialog with the given result. If a dialog is already open,
     * refreshes its contents and brings it to the front instead of creating a new one.
     *
     * @param result the extreme-condition test result to display
     */
    public static void showOrUpdate(ExtremeConditionResult result) {
        if (openInstance != null && openInstance.isShowing()) {
            openInstance.updateResult(result);
            Stage window = (Stage) openInstance.getDialogPane().getScene().getWindow();
            window.toFront();
            window.requestFocus();
            return;
        }
        ExtremeConditionDialog dialog = new ExtremeConditionDialog(result);
        openInstance = dialog;
        dialog.show();
    }

    /**
     * Returns the currently open dialog, or {@code null} if none is showing.
     * Visible for testing.
     */
    static ExtremeConditionDialog getOpenInstance() {
        if (openInstance != null && !openInstance.isShowing()) {
            openInstance = null;
        }
        return openInstance;
    }

    public ExtremeConditionDialog(ExtremeConditionResult result) {
        initModality(Modality.NONE);
        setTitle("Extreme Condition Test Results");
        this.currentResult = result;

        table = new TableView<>();
        table.setId("extremeConditionTable");
        table.setPlaceholder(new Label("No issues found. Model is robust under extreme conditions."));

        TableColumn<ExtremeConditionFinding, String> paramCol = new TableColumn<>("Parameter");
        paramCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().parameterName()));
        paramCol.setPrefWidth(130);

        TableColumn<ExtremeConditionFinding, String> condCol = new TableColumn<>("Condition");
        condCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().condition().label()));
        condCol.setPrefWidth(80);

        TableColumn<ExtremeConditionFinding, String> valueCol = new TableColumn<>("Applied Value");
        valueCol.setCellValueFactory(data ->
                new SimpleStringProperty(formatValue(data.getValue().appliedValue())));
        valueCol.setPrefWidth(100);

        TableColumn<ExtremeConditionFinding, String> varCol = new TableColumn<>("Affected Variable");
        varCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().affectedVariable()));
        varCol.setPrefWidth(130);

        TableColumn<ExtremeConditionFinding, String> stepCol = new TableColumn<>("Step");
        stepCol.setCellValueFactory(data -> {
            long step = data.getValue().stepNumber();
            return new SimpleStringProperty(step >= 0 ? Long.toString(step) : "N/A");
        });
        stepCol.setPrefWidth(60);

        TableColumn<ExtremeConditionFinding, String> descCol = new TableColumn<>("Description");
        descCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().description()));
        descCol.setPrefWidth(300);

        table.getColumns().add(paramCol);
        table.getColumns().add(condCol);
        table.getColumns().add(valueCol);
        table.getColumns().add(varCol);
        table.getColumns().add(stepCol);
        table.getColumns().add(descCol);

        table.setItems(FXCollections.observableArrayList(result.findings()));

        summaryLabel = new Label();
        summaryLabel.setId("extremeConditionSummary");
        summaryLabel.setPadding(new Insets(6, 8, 6, 8));
        updateSummaryLabel(result);

        copyButton = new Button("Copy to Clipboard");
        copyButton.setId("extremeConditionCopy");
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
        getDialogPane().setPrefWidth(Styles.screenAwareWidth(800));
        getDialogPane().setPrefHeight(Styles.screenAwareHeight(500));
        getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        setResultConverter(button -> null);

        setOnHidden(e -> {
            if (openInstance == this) {
                openInstance = null;
            }
        });
    }

    /**
     * Replaces the displayed result with a new one.
     *
     * @param result the new extreme-condition test result
     */
    public void updateResult(ExtremeConditionResult result) {
        this.currentResult = result;
        table.setItems(FXCollections.observableArrayList(result.findings()));
        updateSummaryLabel(result);
        copyButton.setText("Copy to Clipboard");
    }

    private void updateSummaryLabel(ExtremeConditionResult result) {
        if (result.findings().isEmpty()) {
            summaryLabel.setText("No issues found (" + result.runsCompleted() + " runs)");
        } else {
            summaryLabel.setText(result.findings().size() + " findings from "
                    + result.runsCompleted() + " runs");
        }
    }

    private static String formatValue(double value) {
        if (!Double.isInfinite(value) && !Double.isNaN(value)
                && Double.compare(value, Math.rint(value)) == 0
                && Math.abs(value) <= Long.MAX_VALUE) {
            return Long.toString((long) value);
        }
        return Double.toString(value);
    }

    private static String formatAsText(ExtremeConditionResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("Extreme Condition Test Results\n");
        sb.append("==============================\n\n");
        if (result.findings().isEmpty()) {
            sb.append("No issues found. Model is robust under extreme conditions.\n");
        } else {
            sb.append(result.findings().size()).append(" findings from ")
                    .append(result.runsCompleted()).append(" runs\n\n");
            for (ExtremeConditionFinding f : result.findings()) {
                sb.append("[").append(f.condition().label()).append("] ");
                sb.append(f.parameterName()).append(" = ").append(formatValue(f.appliedValue()));
                sb.append(": ").append(f.description()).append("\n");
            }
        }
        return sb.toString();
    }
}

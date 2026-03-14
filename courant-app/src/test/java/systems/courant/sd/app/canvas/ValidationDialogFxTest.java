package systems.courant.sd.app.canvas;

import systems.courant.sd.model.def.ValidationIssue;
import systems.courant.sd.model.def.ValidationIssue.Severity;
import systems.courant.sd.model.def.ValidationResult;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ValidationDialog (TestFX)")
@ExtendWith(ApplicationExtension.class)
class ValidationDialogFxTest {

    private ValidationDialog validationDialog;

    @Start
    void start(Stage stage) {
        stage.setScene(new Scene(new StackPane(), 200, 200));
        stage.show();
    }

    private void showDialog(ValidationResult result) {
        showDialog(result, null);
    }

    private void showDialog(ValidationResult result, java.util.function.Consumer<String> callback) {
        Platform.runLater(() -> {
            validationDialog = new ValidationDialog(result, callback);
            validationDialog.show();
        });
        WaitForAsyncUtils.waitForFxEvents();
    }

    @Test
    @DisplayName("Shows issues in table")
    void showsIssuesInTable(FxRobot robot) {
        ValidationResult result = new ValidationResult(List.of(
                new ValidationIssue(Severity.ERROR, "Stock 1", "Missing equation"),
                new ValidationIssue(Severity.WARNING, "Flow 1", "Unused flow")
        ));
        showDialog(result);

        @SuppressWarnings("unchecked")
        TableView<ValidationIssue> table = robot.lookup("#validationTable")
                .queryAs(TableView.class);
        assertThat(table.getItems()).hasSize(2);
    }

    @Test
    @DisplayName("Shows summary with error and warning counts")
    void showsSummary(FxRobot robot) {
        ValidationResult result = new ValidationResult(List.of(
                new ValidationIssue(Severity.ERROR, "S1", "Bad"),
                new ValidationIssue(Severity.ERROR, "S2", "Also bad"),
                new ValidationIssue(Severity.WARNING, "F1", "Meh")
        ));
        showDialog(result);

        Label summary = robot.lookup("#validationSummary").queryAs(Label.class);
        assertThat(summary.getText()).contains("2 errors");
    }

    @Test
    @DisplayName("Shows 'No issues found' for clean result")
    void showsCleanMessage(FxRobot robot) {
        showDialog(new ValidationResult(List.of()));

        Label summary = robot.lookup("#validationSummary").queryAs(Label.class);
        assertThat(summary.getText()).contains("No issues found");
    }

    @Test
    @DisplayName("Clicking a row invokes the selection callback")
    void clickRowInvokesCallback(FxRobot robot) {
        String[] selected = {null};
        ValidationResult result = new ValidationResult(List.of(
                new ValidationIssue(Severity.ERROR, "MyStock", "Error here")
        ));
        showDialog(result, name -> selected[0] = name);

        @SuppressWarnings("unchecked")
        TableView<ValidationIssue> table = robot.lookup("#validationTable")
                .queryAs(TableView.class);

        Platform.runLater(() -> table.getSelectionModel().select(0));
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(selected[0]).isEqualTo("MyStock");
    }

    @Test
    @DisplayName("showOrUpdate updates the callback when dialog is reused (#379)")
    void shouldUpdateCallbackOnReuse(FxRobot robot) {
        String[] selectedByA = {null};
        String[] selectedByB = {null};

        ValidationResult result1 = new ValidationResult(List.of(
                new ValidationIssue(Severity.ERROR, "StockA", "Error A")
        ));

        // Window A opens the dialog
        Platform.runLater(() -> ValidationDialog.showOrUpdate(result1, name -> selectedByA[0] = name));
        WaitForAsyncUtils.waitForFxEvents();

        // Window B calls showOrUpdate with a different callback and result
        ValidationResult result2 = new ValidationResult(List.of(
                new ValidationIssue(Severity.ERROR, "StockB", "Error B")
        ));
        Platform.runLater(() -> ValidationDialog.showOrUpdate(result2, name -> selectedByB[0] = name));
        WaitForAsyncUtils.waitForFxEvents();

        // Select row 0 — should invoke B's callback, not A's
        @SuppressWarnings("unchecked")
        TableView<ValidationIssue> table = robot.lookup("#validationTable")
                .queryAs(TableView.class);
        Platform.runLater(() -> table.getSelectionModel().select(0));
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(selectedByA[0]).isNull();
        assertThat(selectedByB[0]).isEqualTo("StockB");

        // Clean up
        ValidationDialog instance = ValidationDialog.getOpenInstance();
        if (instance != null) {
            Platform.runLater(instance::close);
            WaitForAsyncUtils.waitForFxEvents();
        }
    }

    @Test
    @DisplayName("Dialog title is 'Model Validation'")
    void dialogTitle(FxRobot robot) {
        showDialog(new ValidationResult(List.of()));

        assertThat(validationDialog.getTitle()).isEqualTo("Model Validation");
    }

    @Test
    @DisplayName("Dialog has a Close button (#213)")
    void hasCloseButton(FxRobot robot) {
        showDialog(new ValidationResult(List.of()));

        DialogPane pane = validationDialog.getDialogPane();
        assertThat(pane.getButtonTypes()).contains(javafx.scene.control.ButtonType.CLOSE);
    }

    @Test
    @DisplayName("Dialog uses screen-aware width (#202)")
    void usesScreenAwareWidth(FxRobot robot) {
        showDialog(new ValidationResult(List.of()));

        double prefWidth = validationDialog.getDialogPane().getPrefWidth();
        assertThat(prefWidth).isLessThanOrEqualTo(700);
        assertThat(prefWidth).isGreaterThan(0);
    }

    @Test
    @DisplayName("Table, summary and copy button have fx:id values (#408)")
    void controlsHaveFxIds(FxRobot robot) {
        showDialog(new ValidationResult(List.of(
                new ValidationIssue(Severity.ERROR, "S1", "Err")
        )));

        assertThat(robot.lookup("#validationTable").tryQuery()).isPresent();
        assertThat(robot.lookup("#validationSummary").tryQuery()).isPresent();
        assertThat(robot.lookup("#validationCopy").tryQuery()).isPresent();
    }
}

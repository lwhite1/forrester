package com.deathrayresearch.forrester.app.canvas;

import com.deathrayresearch.forrester.model.def.ValidationIssue;
import com.deathrayresearch.forrester.model.def.ValidationIssue.Severity;
import com.deathrayresearch.forrester.model.def.ValidationResult;

import javafx.application.Platform;
import javafx.scene.Scene;
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
import java.util.Set;

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
        TableView<ValidationIssue> table = robot.lookup(".table-view")
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

        Set<Label> labels = robot.lookup(".label").queryAllAs(Label.class);
        boolean foundSummary = labels.stream()
                .anyMatch(l -> l.getText() != null && l.getText().contains("2 errors"));
        assertThat(foundSummary).isTrue();
    }

    @Test
    @DisplayName("Shows 'No issues found' for clean result")
    void showsCleanMessage(FxRobot robot) {
        showDialog(new ValidationResult(List.of()));

        Set<Label> labels = robot.lookup(".label").queryAllAs(Label.class);
        boolean found = labels.stream()
                .anyMatch(l -> l.getText() != null && l.getText().contains("No issues found"));
        assertThat(found).isTrue();
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
        TableView<ValidationIssue> table = robot.lookup(".table-view")
                .queryAs(TableView.class);

        Platform.runLater(() -> table.getSelectionModel().select(0));
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(selected[0]).isEqualTo("MyStock");
    }

    @Test
    @DisplayName("Dialog title is 'Model Validation'")
    void dialogTitle(FxRobot robot) {
        showDialog(new ValidationResult(List.of()));

        assertThat(validationDialog.getTitle()).isEqualTo("Model Validation");
    }
}

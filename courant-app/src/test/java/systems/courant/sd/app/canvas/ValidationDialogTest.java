package systems.courant.sd.app.canvas;

import systems.courant.sd.model.def.ValidationIssue;
import systems.courant.sd.model.def.ValidationResult;

import javafx.application.Platform;
import javafx.stage.Stage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ValidationDialog")
@ExtendWith(ApplicationExtension.class)
class ValidationDialogTest {

    @Start
    void start(Stage stage) {
        // No-op — tests create their own ValidationDialog instances
    }

    private ValidationResult resultWith(int errors, int warnings) {
        List<ValidationIssue> issues = new java.util.ArrayList<>();
        for (int i = 0; i < errors; i++) {
            issues.add(new ValidationIssue(
                    ValidationIssue.Severity.ERROR, "Element" + i, "Error " + i));
        }
        for (int i = 0; i < warnings; i++) {
            issues.add(new ValidationIssue(
                    ValidationIssue.Severity.WARNING, "Element" + i, "Warning " + i));
        }
        return new ValidationResult(issues);
    }

    @Test
    @DisplayName("showOrUpdate creates a new dialog when none is open")
    void shouldCreateNewDialogWhenNoneOpen() {
        assertThat(ValidationDialog.getOpenInstance()).isNull();

        ValidationResult result = resultWith(1, 0);
        Platform.runLater(() -> ValidationDialog.showOrUpdate(result, name -> {}));
        WaitForAsyncUtils.waitForFxEvents();

        ValidationDialog instance = ValidationDialog.getOpenInstance();
        assertThat(instance).isNotNull();
        assertThat(instance.isShowing()).isTrue();

        Platform.runLater(() -> instance.close());
        WaitForAsyncUtils.waitForFxEvents();
    }

    @Test
    @DisplayName("showOrUpdate reuses existing dialog instead of creating a new one")
    void shouldReuseExistingDialog() {
        ValidationResult result1 = resultWith(1, 0);
        Platform.runLater(() -> ValidationDialog.showOrUpdate(result1, name -> {}));
        WaitForAsyncUtils.waitForFxEvents();

        ValidationDialog firstInstance = ValidationDialog.getOpenInstance();
        assertThat(firstInstance).isNotNull();

        // Call showOrUpdate again — should reuse, not create new
        ValidationResult result2 = resultWith(2, 1);
        Platform.runLater(() -> ValidationDialog.showOrUpdate(result2, name -> {}));
        WaitForAsyncUtils.waitForFxEvents();

        ValidationDialog secondInstance = ValidationDialog.getOpenInstance();
        assertThat(secondInstance).isSameAs(firstInstance);

        Platform.runLater(() -> firstInstance.close());
        WaitForAsyncUtils.waitForFxEvents();
    }

    @Test
    @DisplayName("Closing the dialog clears the open instance")
    void shouldClearInstanceOnClose() {
        ValidationResult result = resultWith(1, 0);
        Platform.runLater(() -> ValidationDialog.showOrUpdate(result, name -> {}));
        WaitForAsyncUtils.waitForFxEvents();

        ValidationDialog instance = ValidationDialog.getOpenInstance();
        assertThat(instance).isNotNull();

        Platform.runLater(() -> instance.close());
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(ValidationDialog.getOpenInstance()).isNull();
    }

    @Test
    @DisplayName("showOrUpdate creates a new dialog after previous one was closed")
    void shouldCreateNewDialogAfterPreviousClosed() {
        ValidationResult result = resultWith(1, 0);

        Platform.runLater(() -> ValidationDialog.showOrUpdate(result, name -> {}));
        WaitForAsyncUtils.waitForFxEvents();
        ValidationDialog first = ValidationDialog.getOpenInstance();
        Platform.runLater(() -> first.close());
        WaitForAsyncUtils.waitForFxEvents();

        Platform.runLater(() -> ValidationDialog.showOrUpdate(result, name -> {}));
        WaitForAsyncUtils.waitForFxEvents();
        ValidationDialog second = ValidationDialog.getOpenInstance();

        assertThat(second).isNotNull();
        assertThat(second).isNotSameAs(first);

        Platform.runLater(() -> second.close());
        WaitForAsyncUtils.waitForFxEvents();
    }

    @Test
    @DisplayName("updateResult refreshes the displayed content")
    void shouldUpdateResultContent() {
        ValidationResult result1 = resultWith(1, 0);
        Platform.runLater(() -> ValidationDialog.showOrUpdate(result1, name -> {}));
        WaitForAsyncUtils.waitForFxEvents();

        ValidationDialog instance = ValidationDialog.getOpenInstance();
        assertThat(instance).isNotNull();

        // Update with a different result
        ValidationResult result2 = resultWith(3, 2);
        Platform.runLater(() -> instance.updateResult(result2));
        WaitForAsyncUtils.waitForFxEvents();

        // The dialog should still be the same instance, showing updated data
        assertThat(ValidationDialog.getOpenInstance()).isSameAs(instance);
        assertThat(instance.isShowing()).isTrue();

        Platform.runLater(() -> instance.close());
        WaitForAsyncUtils.waitForFxEvents();
    }

    @Test
    @DisplayName("ValidationDialog extends Dialog, not Stage (#213)")
    void shouldExtendDialog() {
        ValidationResult result = resultWith(1, 0);
        Platform.runLater(() -> ValidationDialog.showOrUpdate(result, name -> {}));
        WaitForAsyncUtils.waitForFxEvents();

        ValidationDialog instance = ValidationDialog.getOpenInstance();
        assertThat(instance).isInstanceOf(javafx.scene.control.Dialog.class);

        Platform.runLater(() -> instance.close());
        WaitForAsyncUtils.waitForFxEvents();
    }
}

package systems.courant.sd.app.canvas.dialogs;

import systems.courant.sd.sweep.ExtremeCondition;
import systems.courant.sd.sweep.ExtremeConditionFinding;
import systems.courant.sd.sweep.ExtremeConditionResult;

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

@DisplayName("ExtremeConditionDialog")
@ExtendWith(ApplicationExtension.class)
class ExtremeConditionDialogTest {

    @Start
    void start(Stage stage) {
        // No-op — tests create their own dialog instances
    }

    private ExtremeConditionResult resultWith(int findingCount) {
        List<ExtremeConditionFinding> findings = new java.util.ArrayList<>();
        for (int i = 0; i < findingCount; i++) {
            findings.add(new ExtremeConditionFinding(
                    "param" + i, 1.0 + i, ExtremeCondition.ZERO, 0.0,
                    "Stock" + i, i, "Test finding " + i));
        }
        return new ExtremeConditionResult(findings, 6, 6);
    }

    @Test
    @DisplayName("showOrUpdate creates a new dialog when none is open")
    void shouldCreateNewDialogWhenNoneOpen() {
        assertThat(ExtremeConditionDialog.getOpenInstance()).isNull();

        ExtremeConditionResult result = resultWith(1);
        Platform.runLater(() -> ExtremeConditionDialog.showOrUpdate(result));
        WaitForAsyncUtils.waitForFxEvents();

        ExtremeConditionDialog instance = ExtremeConditionDialog.getOpenInstance();
        assertThat(instance).isNotNull();
        assertThat(instance.isShowing()).isTrue();

        Platform.runLater(() -> instance.close());
        WaitForAsyncUtils.waitForFxEvents();
    }

    @Test
    @DisplayName("showOrUpdate reuses existing dialog instead of creating a new one")
    void shouldReuseExistingDialog() {
        ExtremeConditionResult result1 = resultWith(1);
        Platform.runLater(() -> ExtremeConditionDialog.showOrUpdate(result1));
        WaitForAsyncUtils.waitForFxEvents();

        ExtremeConditionDialog firstInstance = ExtremeConditionDialog.getOpenInstance();
        assertThat(firstInstance).isNotNull();

        ExtremeConditionResult result2 = resultWith(3);
        Platform.runLater(() -> ExtremeConditionDialog.showOrUpdate(result2));
        WaitForAsyncUtils.waitForFxEvents();

        ExtremeConditionDialog secondInstance = ExtremeConditionDialog.getOpenInstance();
        assertThat(secondInstance).isSameAs(firstInstance);

        Platform.runLater(() -> firstInstance.close());
        WaitForAsyncUtils.waitForFxEvents();
    }

    @Test
    @DisplayName("Closing the dialog clears the open instance")
    void shouldClearInstanceOnClose() {
        ExtremeConditionResult result = resultWith(1);
        Platform.runLater(() -> ExtremeConditionDialog.showOrUpdate(result));
        WaitForAsyncUtils.waitForFxEvents();

        ExtremeConditionDialog instance = ExtremeConditionDialog.getOpenInstance();
        assertThat(instance).isNotNull();

        Platform.runLater(() -> instance.close());
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(ExtremeConditionDialog.getOpenInstance()).isNull();
    }

    @Test
    @DisplayName("showOrUpdate creates a new dialog after previous one was closed")
    void shouldCreateNewDialogAfterPreviousClosed() {
        ExtremeConditionResult result = resultWith(1);

        Platform.runLater(() -> ExtremeConditionDialog.showOrUpdate(result));
        WaitForAsyncUtils.waitForFxEvents();
        ExtremeConditionDialog first = ExtremeConditionDialog.getOpenInstance();
        Platform.runLater(() -> first.close());
        WaitForAsyncUtils.waitForFxEvents();

        Platform.runLater(() -> ExtremeConditionDialog.showOrUpdate(result));
        WaitForAsyncUtils.waitForFxEvents();
        ExtremeConditionDialog second = ExtremeConditionDialog.getOpenInstance();

        assertThat(second).isNotNull();
        assertThat(second).isNotSameAs(first);

        Platform.runLater(() -> second.close());
        WaitForAsyncUtils.waitForFxEvents();
    }

    @Test
    @DisplayName("updateResult refreshes the displayed content")
    void shouldUpdateResultContent() {
        ExtremeConditionResult result1 = resultWith(1);
        Platform.runLater(() -> ExtremeConditionDialog.showOrUpdate(result1));
        WaitForAsyncUtils.waitForFxEvents();

        ExtremeConditionDialog instance = ExtremeConditionDialog.getOpenInstance();
        assertThat(instance).isNotNull();

        ExtremeConditionResult result2 = resultWith(5);
        Platform.runLater(() -> instance.updateResult(result2));
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(ExtremeConditionDialog.getOpenInstance()).isSameAs(instance);
        assertThat(instance.isShowing()).isTrue();

        Platform.runLater(() -> instance.close());
        WaitForAsyncUtils.waitForFxEvents();
    }

    @Test
    @DisplayName("Dialog shows empty state when no findings")
    void shouldShowEmptyState() {
        ExtremeConditionResult result = new ExtremeConditionResult(List.of(), 6, 6);
        Platform.runLater(() -> ExtremeConditionDialog.showOrUpdate(result));
        WaitForAsyncUtils.waitForFxEvents();

        ExtremeConditionDialog instance = ExtremeConditionDialog.getOpenInstance();
        assertThat(instance).isNotNull();
        assertThat(instance.isShowing()).isTrue();

        Platform.runLater(() -> instance.close());
        WaitForAsyncUtils.waitForFxEvents();
    }

    @Test
    @DisplayName("ExtremeConditionDialog extends Dialog")
    void shouldExtendDialog() {
        ExtremeConditionResult result = resultWith(1);
        Platform.runLater(() -> ExtremeConditionDialog.showOrUpdate(result));
        WaitForAsyncUtils.waitForFxEvents();

        ExtremeConditionDialog instance = ExtremeConditionDialog.getOpenInstance();
        assertThat(instance).isInstanceOf(javafx.scene.control.Dialog.class);

        Platform.runLater(() -> instance.close());
        WaitForAsyncUtils.waitForFxEvents();
    }
}

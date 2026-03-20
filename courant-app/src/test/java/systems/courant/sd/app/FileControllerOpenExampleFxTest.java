package systems.courant.sd.app;

import systems.courant.sd.app.canvas.Clipboard;

import javafx.application.Platform;
import javafx.stage.Stage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FileController.openExample() guards unsaved changes (#463)")
@ExtendWith(ApplicationExtension.class)
class FileControllerOpenExampleFxTest {

    private ModelWindow window;
    private Stage stage;

    @Start
    void start(Stage stage) {
        this.stage = stage;
        CourantApp app = new CourantApp();
        window = new ModelWindow(stage, app, new Clipboard());
        stage.show();
    }

    /**
     * Waits for the background auto-layout thread to complete and for the
     * resulting {@code Platform.runLater(applyView)} to be processed.
     */
    private void awaitLayout() {
        try {
            window.layoutFuture().get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException("Layout did not complete in time", e);
        }
        WaitForAsyncUtils.waitForFxEvents();
    }

    @Test
    @DisplayName("openExample loads model when no unsaved changes exist")
    void shouldLoadExampleWhenClean(FxRobot robot) {
        assertThat(window.isDirty()).isFalse();

        Platform.runLater(() ->
                window.getFileController().openExample("Bathtub", "introductory/bathtub.json"));
        WaitForAsyncUtils.waitForFxEvents();
        awaitLayout();

        assertThat(stage.getTitle()).contains("Bathtub");
        assertThat(window.isDirty()).isFalse();
        assertThat(window.getCurrentFile()).isNull();
    }

    @Test
    @DisplayName("openExample shows confirmation dialog when dirty and cancelling preserves model")
    void shouldPromptWhenDirtyAndCancelPreservesState(FxRobot robot) {
        // Load an initial example so we have a known title
        Platform.runLater(() ->
                window.getFileController().openExample("Bathtub", "introductory/bathtub.json"));
        WaitForAsyncUtils.waitForFxEvents();
        awaitLayout();
        assertThat(stage.getTitle()).contains("Bathtub");

        // Mark dirty
        Platform.runLater(() -> window.getFileController().markDirty());
        WaitForAsyncUtils.waitForFxEvents();
        assertThat(window.isDirty()).isTrue();

        // Attempt to open another example — dialog should appear
        Platform.runLater(() ->
                window.getFileController().openExample("SIR Epidemic", "epidemiology/sir-epidemic.json"));
        WaitForAsyncUtils.waitForFxEvents();

        // Click Cancel on the confirmation dialog
        robot.clickOn("Cancel");
        WaitForAsyncUtils.waitForFxEvents();

        // Model should NOT have changed — still showing Bathtub, still dirty
        assertThat(stage.getTitle()).contains("Bathtub");
        assertThat(window.isDirty()).isTrue();
    }

    @Test
    @DisplayName("openExample proceeds when dirty and user confirms discard")
    void shouldLoadExampleWhenDirtyAndUserConfirms(FxRobot robot) {
        // Load an initial example
        Platform.runLater(() ->
                window.getFileController().openExample("Bathtub", "introductory/bathtub.json"));
        WaitForAsyncUtils.waitForFxEvents();
        awaitLayout();

        // Mark dirty
        Platform.runLater(() -> window.getFileController().markDirty());
        WaitForAsyncUtils.waitForFxEvents();
        assertThat(window.isDirty()).isTrue();

        // Attempt to open another example — dialog should appear
        Platform.runLater(() ->
                window.getFileController().openExample("SIR Epidemic", "epidemiology/sir-epidemic.json"));
        WaitForAsyncUtils.waitForFxEvents();

        // Click OK to discard changes
        robot.clickOn("OK");
        WaitForAsyncUtils.waitForFxEvents();
        awaitLayout();

        // Model should now show the new example (title may truncate long names)
        assertThat(stage.getTitle()).contains("SIR");
        assertThat(window.isDirty()).isFalse();
        assertThat(window.getCurrentFile()).isNull();
    }

    @Test
    @DisplayName("openExample clears currentFile after loading")
    void shouldClearCurrentFileAfterLoadingExample(FxRobot robot) {
        // Set a fake current file path
        Platform.runLater(() ->
                window.getFileController().setCurrentFile(java.nio.file.Path.of("fake.json")));
        WaitForAsyncUtils.waitForFxEvents();
        assertThat(window.getCurrentFile()).isNotNull();

        Platform.runLater(() ->
                window.getFileController().openExample("Bathtub", "introductory/bathtub.json"));
        WaitForAsyncUtils.waitForFxEvents();
        awaitLayout();

        assertThat(window.getCurrentFile()).isNull();
    }
}

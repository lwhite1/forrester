package systems.courant.sd.app;

import systems.courant.sd.app.canvas.Clipboard;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end workflow tests for file operations:
 * new, open examples, dirty state, discard changes, and switching models.
 */
@DisplayName("Workflow: File Operations (TestFX)")
@ExtendWith(ApplicationExtension.class)
class WorkflowFileOperationsFxTest {

    private ModelWindow window;
    private Stage stage;

    @Start
    void start(Stage stage) {
        this.stage = stage;
        CourantApp app = new CourantApp();
        window = new ModelWindow(stage, app, new Clipboard());
        stage.show();
    }

    private void loadExample(String name, String resourcePath) {
        Platform.runLater(() ->
                window.getFileController().openExample(name, resourcePath));
        WaitForAsyncUtils.waitForFxEvents();
        try {
            window.layoutFuture().get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException("Layout did not complete", e);
        }
        WaitForAsyncUtils.waitForFxEvents();
    }

    private void waitForDashboardResults(FxRobot robot) throws TimeoutException {
        WaitForAsyncUtils.waitFor(30, TimeUnit.SECONDS, () -> {
            WaitForAsyncUtils.waitForFxEvents();
            var tabs = robot.lookup("#dashboardResultTabs").tryQueryAs(TabPane.class);
            return tabs.isPresent() && tabs.get().isVisible() && !tabs.get().getTabs().isEmpty();
        });
    }

    // ── New model ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Ctrl+N creates a fresh Untitled model")
    void shouldCreateUntitledModel(FxRobot robot) {
        loadExample("Bathtub", "introductory/bathtub.json");
        assertThat(stage.getTitle()).contains("Bathtub");

        robot.push(KeyCode.CONTROL, KeyCode.N);
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(stage.getTitle()).contains("Untitled");
        Label elementsLabel = robot.lookup("#statusElements").queryAs(Label.class);
        assertThat(elementsLabel.getText()).isEqualTo("Empty model");
    }

    @Test
    @DisplayName("New model on dirty state shows confirmation, Cancel preserves")
    void shouldPreserveDirtyModelOnCancel(FxRobot robot) {
        loadExample("Bathtub", "introductory/bathtub.json");
        Platform.runLater(() -> window.getFileController().markDirty());
        WaitForAsyncUtils.waitForFxEvents();

        robot.push(KeyCode.CONTROL, KeyCode.N);
        WaitForAsyncUtils.waitForFxEvents();

        robot.clickOn("Cancel");
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(stage.getTitle()).contains("Bathtub");
        assertThat(window.isDirty()).isTrue();
    }

    @Test
    @DisplayName("New model on dirty state, OK discards and clears")
    void shouldDiscardAndCreateNew(FxRobot robot) {
        loadExample("Bathtub", "introductory/bathtub.json");
        Platform.runLater(() -> window.getFileController().markDirty());
        WaitForAsyncUtils.waitForFxEvents();

        robot.push(KeyCode.CONTROL, KeyCode.N);
        WaitForAsyncUtils.waitForFxEvents();

        robot.clickOn("OK");
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(window.isDirty()).isFalse();
        assertThat(stage.getTitle()).contains("Untitled");
    }

    @Test
    @DisplayName("New model clears dashboard results")
    void shouldClearDashboardOnNew(FxRobot robot) throws Exception {
        loadExample("Bathtub", "introductory/bathtub.json");

        robot.push(KeyCode.CONTROL, KeyCode.R);
        waitForDashboardResults(robot);

        robot.push(KeyCode.CONTROL, KeyCode.N);
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(robot.lookup("#dashboardResultTabs").queryAs(TabPane.class)
                .isVisible()).isFalse();
    }

    // ── Dirty state tracking ─────────────────────────────────────────────

    @Test
    @DisplayName("Model starts clean after loading example")
    void shouldBeCleanAfterLoad(FxRobot robot) {
        loadExample("Bathtub", "introductory/bathtub.json");
        assertThat(window.isDirty()).isFalse();
    }

    @Test
    @DisplayName("Editing equation marks model as dirty")
    void shouldBeDirtyAfterEquationChange(FxRobot robot) {
        loadExample("Bathtub", "introductory/bathtub.json");

        Platform.runLater(() -> window.getEditor().setVariableEquation("Outflow Rate", "10"));
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(window.isDirty()).isTrue();
    }

    @Test
    @DisplayName("Dirty indicator appears in window title")
    void shouldShowDirtyIndicatorInTitle(FxRobot robot) {
        loadExample("Bathtub", "introductory/bathtub.json");
        String cleanTitle = stage.getTitle();

        Platform.runLater(() -> window.getFileController().markDirty());
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(stage.getTitle()).isNotEqualTo(cleanTitle);
    }

    // ── Unsaved changes guard ────────────────────────────────────────────

    @Test
    @DisplayName("Cancel on unsaved changes preserves dirty model")
    void shouldCancelPreservesDirtyModel(FxRobot robot) {
        loadExample("Bathtub", "introductory/bathtub.json");
        Platform.runLater(() -> window.getFileController().markDirty());
        WaitForAsyncUtils.waitForFxEvents();

        Platform.runLater(() -> window.getFileController()
                .openExample("Coffee Cooling", "introductory/coffee-cooling.json"));
        WaitForAsyncUtils.waitForFxEvents();

        robot.clickOn("Cancel");
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(stage.getTitle()).contains("Bathtub");
        assertThat(window.isDirty()).isTrue();
    }

    @Test
    @DisplayName("OK on unsaved changes discards and loads new model")
    void shouldDiscardAndLoadNew(FxRobot robot) {
        loadExample("Bathtub", "introductory/bathtub.json");
        Platform.runLater(() -> window.getFileController().markDirty());
        WaitForAsyncUtils.waitForFxEvents();

        Platform.runLater(() -> window.getFileController()
                .openExample("Coffee Cooling", "introductory/coffee-cooling.json"));
        WaitForAsyncUtils.waitForFxEvents();

        robot.clickOn("OK");
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(stage.getTitle()).contains("Coffee Cooling");
        assertThat(window.isDirty()).isFalse();
    }

    // ── Sequential model loading ─────────────────────────────────────────

    @Test
    @DisplayName("Can load multiple examples in sequence")
    void shouldLoadMultipleExamples(FxRobot robot) {
        loadExample("Bathtub", "introductory/bathtub.json");
        assertThat(stage.getTitle()).contains("Bathtub");

        loadExample("Coffee Cooling", "introductory/coffee-cooling.json");
        assertThat(stage.getTitle()).contains("Coffee Cooling");

        loadExample("Exponential Growth", "introductory/exponential-growth.json");
        assertThat(stage.getTitle()).contains("Exponential Growth");

        loadExample("Goal Seeking", "introductory/goal-seeking.json");
        assertThat(stage.getTitle()).contains("Goal Seeking");
    }

    @Test
    @DisplayName("Element count updates when switching models")
    void shouldUpdateElementCount(FxRobot robot) {
        loadExample("Bathtub", "introductory/bathtub.json");
        String bathtubElements = robot.lookup("#statusElements")
                .queryAs(Label.class).getText();

        loadExample("Coffee Cooling", "introductory/coffee-cooling.json");
        String coffeeElements = robot.lookup("#statusElements")
                .queryAs(Label.class).getText();

        assertThat(bathtubElements).isNotEqualTo(coffeeElements);
    }

    @Test
    @DisplayName("Properties panel shows model name after loading")
    void shouldShowModelNameInProperties(FxRobot robot) {
        loadExample("Bathtub", "introductory/bathtub.json");

        TextField nameField = robot.lookup("#modelNameField").queryAs(TextField.class);
        assertThat(nameField.getText()).isEqualTo("Bathtub");
    }

    // ── File tracking ────────────────────────────────────────────────────

    @Test
    @DisplayName("Example models have null currentFile")
    void shouldHaveNullCurrentFileForExamples(FxRobot robot) {
        loadExample("Bathtub", "introductory/bathtub.json");
        assertThat(window.getCurrentFile()).isNull();
    }
}

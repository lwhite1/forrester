package systems.courant.sd.app;

import systems.courant.sd.app.canvas.Clipboard;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.scene.control.ToggleButton;
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end workflow tests for view and navigation operations:
 * zoom, toolbar switching, activity log, tab switching, and keyboard shortcuts.
 */
@DisplayName("Workflow: View & Navigation (TestFX)")
@ExtendWith(ApplicationExtension.class)
class WorkflowViewNavigationFxTest {

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

    private void waitForDashboardResults(FxRobot robot) {
        long deadline = System.currentTimeMillis() + 15_000;
        while (System.currentTimeMillis() < deadline) {
            WaitForAsyncUtils.waitForFxEvents();
            var tabs = robot.lookup("#dashboardResultTabs").tryQueryAs(TabPane.class);
            if (tabs.isPresent() && tabs.get().isVisible() && !tabs.get().getTabs().isEmpty()) {
                return;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        throw new AssertionError("Dashboard results did not appear within 15 seconds");
    }

    // ── Toolbar tool switching ───────────────────────────────────────────

    @Test
    @DisplayName("Cycling through all tools updates status bar")
    void shouldUpdateStatusForEachTool(FxRobot robot) {
        loadExample("Bathtub", "introductory/bathtub.json");

        Label toolLabel = robot.lookup("#statusTool").queryAs(Label.class);

        robot.clickOn("#toolStock");
        WaitForAsyncUtils.waitForFxEvents();
        assertThat(toolLabel.getText()).isEqualTo("Place Stock");

        robot.clickOn("#toolFlow");
        WaitForAsyncUtils.waitForFxEvents();
        assertThat(toolLabel.getText()).isEqualTo("Place Flow");

        robot.clickOn("#toolAux");
        WaitForAsyncUtils.waitForFxEvents();
        assertThat(toolLabel.getText()).isEqualTo("Place Variable");

        robot.clickOn("#toolModule");
        WaitForAsyncUtils.waitForFxEvents();
        assertThat(toolLabel.getText()).isEqualTo("Place Module");

        robot.clickOn("#toolLookup");
        WaitForAsyncUtils.waitForFxEvents();
        assertThat(toolLabel.getText()).isEqualTo("Place Lookup");

        robot.clickOn("#toolSelect");
        WaitForAsyncUtils.waitForFxEvents();
        assertThat(toolLabel.getText()).isEqualTo("Select");
    }

    @Test
    @DisplayName("Tool buttons are mutually exclusive")
    void shouldExclusivelySelectTools(FxRobot robot) {
        loadExample("Bathtub", "introductory/bathtub.json");

        robot.clickOn("#toolStock");
        WaitForAsyncUtils.waitForFxEvents();

        ToggleButton stockBtn = robot.lookup("#toolStock").queryAs(ToggleButton.class);
        ToggleButton selectBtn = robot.lookup("#toolSelect").queryAs(ToggleButton.class);
        assertThat(stockBtn.isSelected()).isTrue();
        assertThat(selectBtn.isSelected()).isFalse();

        robot.clickOn("#toolFlow");
        WaitForAsyncUtils.waitForFxEvents();

        ToggleButton flowBtn = robot.lookup("#toolFlow").queryAs(ToggleButton.class);
        assertThat(flowBtn.isSelected()).isTrue();
        assertThat(stockBtn.isSelected()).isFalse();
    }

    // ── Zoom operations ──────────────────────────────────────────────────

    @Test
    @DisplayName("Zoom level shows 100% initially")
    void shouldShowDefaultZoom(FxRobot robot) {
        loadExample("Bathtub", "introductory/bathtub.json");

        Label zoomLabel = robot.lookup("#statusZoom").queryAs(Label.class);
        assertThat(zoomLabel.getText()).contains("100%");
    }

    @Test
    @DisplayName("Ctrl+0 resets zoom to 100%")
    void shouldResetZoom(FxRobot robot) {
        loadExample("Bathtub", "introductory/bathtub.json");

        robot.push(KeyCode.CONTROL, KeyCode.SHIFT, KeyCode.F);
        WaitForAsyncUtils.waitForFxEvents();

        robot.push(KeyCode.CONTROL, KeyCode.DIGIT0);
        WaitForAsyncUtils.waitForFxEvents();

        Label zoomLabel = robot.lookup("#statusZoom").queryAs(Label.class);
        assertThat(zoomLabel.getText()).contains("100%");
    }

    // ── Properties/Dashboard tab switching ────────────────────────────────

    @Test
    @DisplayName("Properties tab is selected by default")
    void shouldDefaultToPropertiesTab(FxRobot robot) {
        loadExample("Bathtub", "introductory/bathtub.json");

        TabPane rightTabPane = robot.lookup("#rightTabPane").queryAs(TabPane.class);
        assertThat(rightTabPane.getSelectionModel().getSelectedItem().getText())
                .isEqualTo("Properties");
    }

    @Test
    @DisplayName("Simulation switches to Dashboard, can switch back to Properties")
    void shouldSwitchTabsAfterSimulation(FxRobot robot) {
        loadExample("Bathtub", "introductory/bathtub.json");

        robot.push(KeyCode.CONTROL, KeyCode.R);
        waitForDashboardResults(robot);

        TabPane rightTabPane = robot.lookup("#rightTabPane").queryAs(TabPane.class);
        assertThat(rightTabPane.getSelectionModel().getSelectedItem().getText())
                .isEqualTo("Dashboard");

        Platform.runLater(() -> rightTabPane.getSelectionModel().select(0));
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(rightTabPane.getSelectionModel().getSelectedItem().getText())
                .isEqualTo("Properties");
    }

    // ── Status bar updates ───────────────────────────────────────────────

    @Test
    @DisplayName("Status bar shows element counts for loaded model")
    void shouldShowElementCounts(FxRobot robot) {
        loadExample("Bathtub", "introductory/bathtub.json");

        Label elementsLabel = robot.lookup("#statusElements").queryAs(Label.class);
        assertThat(elementsLabel.getText()).contains("elements");
    }

    @Test
    @DisplayName("Select All updates selection count in status bar")
    void shouldShowSelectionCount(FxRobot robot) {
        loadExample("Bathtub", "introductory/bathtub.json");

        robot.push(KeyCode.CONTROL, KeyCode.A);
        WaitForAsyncUtils.waitForFxEvents();

        Label selectionLabel = robot.lookup("#statusSelection").queryAs(Label.class);
        assertThat(selectionLabel.getText()).contains("selected");
    }

    // ── Full keyboard workflow ───────────────────────────────────────────

    @Test
    @DisplayName("Ctrl+N → load → Ctrl+B → Ctrl+R full keyboard workflow")
    void shouldCompleteFullKeyboardWorkflow(FxRobot robot) {
        robot.push(KeyCode.CONTROL, KeyCode.N);
        WaitForAsyncUtils.waitForFxEvents();
        assertThat(stage.getTitle()).contains("Untitled");

        loadExample("Bathtub", "introductory/bathtub.json");
        assertThat(stage.getTitle()).contains("Bathtub");

        robot.push(KeyCode.CONTROL, KeyCode.B);
        WaitForAsyncUtils.waitForFxEvents();
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        WaitForAsyncUtils.waitForFxEvents();

        Platform.runLater(() -> {
            stage.toFront();
            stage.requestFocus();
            window.getCanvas().requestFocus();
        });
        WaitForAsyncUtils.waitForFxEvents();
        robot.push(KeyCode.CONTROL, KeyCode.R);
        waitForDashboardResults(robot);

        TabPane resultTabs = robot.lookup("#dashboardResultTabs").queryAs(TabPane.class);
        assertThat(resultTabs.getTabs()).isNotEmpty();
    }
}

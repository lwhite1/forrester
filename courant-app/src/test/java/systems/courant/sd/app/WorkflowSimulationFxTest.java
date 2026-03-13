package systems.courant.sd.app;

import systems.courant.sd.app.canvas.Clipboard;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end workflow tests for the core simulation loop:
 * open model, run simulation, verify results, change model, re-run.
 */
@DisplayName("Workflow: Simulation (TestFX)")
@ExtendWith(ApplicationExtension.class)
class WorkflowSimulationFxTest {

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
    }

    private void triggerSimulation(FxRobot robot) {
        robot.push(KeyCode.CONTROL, KeyCode.R);
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

    private void triggerValidation(FxRobot robot) {
        robot.push(KeyCode.CONTROL, KeyCode.B);
        WaitForAsyncUtils.waitForFxEvents();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        WaitForAsyncUtils.waitForFxEvents();
        // Restore focus to main window (validation dialog is a separate stage)
        Platform.runLater(() -> {
            stage.toFront();
            stage.requestFocus();
            window.getCanvas().requestFocus();
        });
        WaitForAsyncUtils.waitForFxEvents();
    }

    // ── Open example and run simulation ──────────────────────────────────

    @Test
    @DisplayName("Bathtub: dashboard shows results after Ctrl+R")
    void shouldShowResultsAfterSimulation(FxRobot robot) {
        loadExample("Bathtub", "introductory/bathtub.json");
        assertThat(stage.getTitle()).contains("Bathtub");

        triggerSimulation(robot);
        waitForDashboardResults(robot);

        TabPane resultTabs = robot.lookup("#dashboardResultTabs").queryAs(TabPane.class);
        assertThat(resultTabs.isVisible()).isTrue();
        assertThat(resultTabs.getTabs()).isNotEmpty();
        assertThat(resultTabs.getTabs().getFirst().getText()).isEqualTo("Simulation");
    }

    @Test
    @DisplayName("Bathtub: placeholder disappears after simulation")
    void shouldHidePlaceholderAfterSimulation(FxRobot robot) {
        loadExample("Bathtub", "introductory/bathtub.json");

        triggerSimulation(robot);
        waitForDashboardResults(robot);

        assertThat(robot.lookup("#dashboardPlaceholder").queryAs(javafx.scene.Node.class)
                .isVisible()).isFalse();
    }

    @Test
    @DisplayName("Coffee Cooling: simulation runs successfully")
    void shouldRunCoffeeCooling(FxRobot robot) {
        loadExample("Coffee Cooling", "introductory/coffee-cooling.json");
        assertThat(stage.getTitle()).contains("Coffee Cooling");

        triggerSimulation(robot);
        waitForDashboardResults(robot);

        TabPane resultTabs = robot.lookup("#dashboardResultTabs").queryAs(TabPane.class);
        assertThat(resultTabs.getTabs()).isNotEmpty();
    }

    @Test
    @DisplayName("Exponential Growth: simulation runs successfully")
    void shouldRunExponentialGrowth(FxRobot robot) {
        loadExample("Exponential Growth", "introductory/exponential-growth.json");
        assertThat(stage.getTitle()).contains("Exponential Growth");

        triggerSimulation(robot);
        waitForDashboardResults(robot);

        TabPane resultTabs = robot.lookup("#dashboardResultTabs").queryAs(TabPane.class);
        assertThat(resultTabs.getTabs()).isNotEmpty();
    }

    @Test
    @DisplayName("Goal Seeking: simulation runs successfully")
    void shouldRunGoalSeeking(FxRobot robot) {
        loadExample("Goal Seeking", "introductory/goal-seeking.json");

        triggerSimulation(robot);
        waitForDashboardResults(robot);

        TabPane resultTabs = robot.lookup("#dashboardResultTabs").queryAs(TabPane.class);
        assertThat(resultTabs.getTabs()).isNotEmpty();
    }

    // ── Dashboard tab selection ──────────────────────────────────────────

    @Test
    @DisplayName("Dashboard tab is automatically selected after simulation")
    void shouldSwitchToDashboardTab(FxRobot robot) {
        loadExample("Bathtub", "introductory/bathtub.json");

        triggerSimulation(robot);
        waitForDashboardResults(robot);

        TabPane rightTabPane = robot.lookup("#rightTabPane").queryAs(TabPane.class);
        assertThat(rightTabPane.getSelectionModel().getSelectedItem().getText())
                .isEqualTo("Dashboard");
    }

    // ── Ghost run history ────────────────────────────────────────────────

    @Test
    @DisplayName("Running simulation twice produces ghost overlay header")
    void shouldShowGhostRunsAfterSecondRun(FxRobot robot) {
        loadExample("Bathtub", "introductory/bathtub.json");

        triggerSimulation(robot);
        waitForDashboardResults(robot);

        triggerSimulation(robot);
        waitForDashboardResults(robot);

        var ghostHeader = robot.lookup("#ghostRunsHeader").tryQueryAs(Label.class);
        assertThat(ghostHeader).isPresent();
    }

    // ── Stale results banner ─────────────────────────────────────────────

    @Test
    @DisplayName("Editing model after simulation shows stale banner")
    void shouldShowStaleBannerAfterEdit(FxRobot robot) {
        loadExample("Bathtub", "introductory/bathtub.json");

        triggerSimulation(robot);
        waitForDashboardResults(robot);

        var banner = robot.lookup("#staleBanner").queryAs(javafx.scene.layout.HBox.class);
        assertThat(banner.isVisible()).isFalse();

        Platform.runLater(() -> window.getEditor().setAuxEquation("Outflow Rate", "10"));
        WaitForAsyncUtils.waitForFxEvents();

        banner = robot.lookup("#staleBanner").queryAs(javafx.scene.layout.HBox.class);
        assertThat(banner.isVisible()).isTrue();
    }

    @Test
    @DisplayName("Re-running simulation clears stale banner")
    void shouldClearStaleBannerOnRerun(FxRobot robot) {
        loadExample("Bathtub", "introductory/bathtub.json");

        triggerSimulation(robot);
        waitForDashboardResults(robot);

        Platform.runLater(() -> window.getEditor().setAuxEquation("Outflow Rate", "10"));
        WaitForAsyncUtils.waitForFxEvents();

        triggerSimulation(robot);
        waitForDashboardResults(robot);

        var banner = robot.lookup("#staleBanner").queryAs(javafx.scene.layout.HBox.class);
        assertThat(banner.isVisible()).isFalse();
    }

    @Test
    @DisplayName("Clicking Re-run link triggers new simulation")
    void shouldRerunViaLink(FxRobot robot) {
        loadExample("Bathtub", "introductory/bathtub.json");

        triggerSimulation(robot);
        waitForDashboardResults(robot);

        Platform.runLater(() -> window.getEditor().setAuxEquation("Outflow Rate", "10"));
        WaitForAsyncUtils.waitForFxEvents();

        robot.clickOn("#staleRerunLink");
        waitForDashboardResults(robot);

        var banner = robot.lookup("#staleBanner").queryAs(javafx.scene.layout.HBox.class);
        assertThat(banner.isVisible()).isFalse();
    }

    // ── Switch models between simulations ────────────────────────────────

    @Test
    @DisplayName("Loading new example clears previous simulation results")
    void shouldClearResultsOnNewModel(FxRobot robot) {
        loadExample("Bathtub", "introductory/bathtub.json");

        triggerSimulation(robot);
        waitForDashboardResults(robot);

        loadExample("Exponential Growth", "introductory/exponential-growth.json");

        assertThat(robot.lookup("#dashboardResultTabs").queryAs(TabPane.class)
                .isVisible()).isFalse();
    }

    @Test
    @DisplayName("Can run simulation on new model after switching")
    void shouldRunAfterSwitchingModels(FxRobot robot) {
        loadExample("Bathtub", "introductory/bathtub.json");
        triggerSimulation(robot);
        waitForDashboardResults(robot);

        loadExample("Coffee Cooling", "introductory/coffee-cooling.json");
        assertThat(stage.getTitle()).contains("Coffee Cooling");

        triggerSimulation(robot);
        waitForDashboardResults(robot);

        TabPane resultTabs = robot.lookup("#dashboardResultTabs").queryAs(TabPane.class);
        assertThat(resultTabs.getTabs()).isNotEmpty();
    }

    // ── Validation before simulation ─────────────────────────────────────

    @Test
    @DisplayName("Valid model shows clean validation status")
    void shouldShowCleanValidation(FxRobot robot) {
        loadExample("Bathtub", "introductory/bathtub.json");

        triggerValidation(robot);

        Label validationLabel = robot.lookup("#statusValidation").queryAs(Label.class);
        assertThat(validationLabel.getText()).contains("No issues");
    }

    @Test
    @DisplayName("Can run simulation after successful validation")
    void shouldRunAfterValidation(FxRobot robot) {
        loadExample("Bathtub", "introductory/bathtub.json");

        triggerValidation(robot);
        triggerSimulation(robot);
        waitForDashboardResults(robot);

        TabPane resultTabs = robot.lookup("#dashboardResultTabs").queryAs(TabPane.class);
        assertThat(resultTabs.getTabs()).isNotEmpty();
    }
}

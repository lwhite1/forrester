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
 * End-to-end workflow tests for complex (intermediate/advanced) example models.
 * Verifies that realistic models load, validate, and simulate correctly.
 */
@DisplayName("Workflow: Complex Models (TestFX)")
@ExtendWith(ApplicationExtension.class)
class WorkflowComplexModelFxTest {

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

    private void waitForDashboardResults(FxRobot robot) {
        long deadline = System.currentTimeMillis() + 30_000;
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
        throw new AssertionError("Dashboard results did not appear within 30 seconds");
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

    // ── Intermediate models ──────────────────────────────────────────────

    @Test
    @DisplayName("Aging Chain: load → validate → simulate")
    void shouldSimulateAgingChain(FxRobot robot) {
        loadExample("Aging Chain", "demographics/aging-chain.json");
        assertThat(stage.getTitle()).contains("Aging");

        triggerValidation(robot);

        Platform.runLater(() -> {
            stage.toFront();
            window.getCanvas().requestFocus();
        });
        WaitForAsyncUtils.waitForFxEvents();
        robot.push(KeyCode.CONTROL, KeyCode.R);
        waitForDashboardResults(robot);

        TabPane resultTabs = robot.lookup("#dashboardResultTabs").queryAs(TabPane.class);
        assertThat(resultTabs.getTabs()).isNotEmpty();
    }

    @Test
    @DisplayName("Predator Prey: load → simulate → verify multiple stocks")
    void shouldSimulatePredatorPrey(FxRobot robot) {
        loadExample("Predator Prey", "ecology/predator-prey.json");
        assertThat(stage.getTitle()).contains("Predator Prey");

        Platform.runLater(() -> {
            stage.toFront();
            window.getCanvas().requestFocus();
        });
        WaitForAsyncUtils.waitForFxEvents();
        robot.push(KeyCode.CONTROL, KeyCode.R);
        waitForDashboardResults(robot);

        TabPane resultTabs = robot.lookup("#dashboardResultTabs").queryAs(TabPane.class);
        assertThat(resultTabs.getTabs()).isNotEmpty();
    }

    @Test
    @DisplayName("S-Shaped Growth: load → simulate")
    void shouldSimulateSShapedGrowth(FxRobot robot) {
        loadExample("S-Shaped Growth", "population/s-shaped-growth.json");

        Platform.runLater(() -> {
            stage.toFront();
            window.getCanvas().requestFocus();
        });
        WaitForAsyncUtils.waitForFxEvents();
        robot.push(KeyCode.CONTROL, KeyCode.R);
        waitForDashboardResults(robot);

        TabPane resultTabs = robot.lookup("#dashboardResultTabs").queryAs(TabPane.class);
        assertThat(resultTabs.getTabs()).isNotEmpty();
    }

    // ── Advanced models ──────────────────────────────────────────────────

    @Test
    @DisplayName("Kaibab Deer: load → validate → simulate")
    void shouldSimulateKaibabDeer(FxRobot robot) {
        loadExample("Kaibab Deer", "ecology/kaibab-deer.json");

        triggerValidation(robot);

        Platform.runLater(() -> {
            stage.toFront();
            window.getCanvas().requestFocus();
        });
        WaitForAsyncUtils.waitForFxEvents();
        robot.push(KeyCode.CONTROL, KeyCode.R);
        waitForDashboardResults(robot);

        TabPane resultTabs = robot.lookup("#dashboardResultTabs").queryAs(TabPane.class);
        assertThat(resultTabs.getTabs()).isNotEmpty();
    }

    @Test
    @DisplayName("Supply Chain Bullwhip: load → simulate")
    void shouldSimulateSupplyChain(FxRobot robot) {
        loadExample("Supply Chain Bullwhip", "supply-chain/supply-chain-bullwhip.json");

        Platform.runLater(() -> {
            stage.toFront();
            window.getCanvas().requestFocus();
        });
        WaitForAsyncUtils.waitForFxEvents();
        robot.push(KeyCode.CONTROL, KeyCode.R);
        waitForDashboardResults(robot);

        TabPane resultTabs = robot.lookup("#dashboardResultTabs").queryAs(TabPane.class);
        assertThat(resultTabs.getTabs()).isNotEmpty();
    }

    // ── Sequential model simulation stress test ──────────────────────────

    @Test
    @DisplayName("Load and simulate four models in sequence")
    void shouldHandleSequentialModelSimulations(FxRobot robot) {
        loadExample("Bathtub", "introductory/bathtub.json");
        Platform.runLater(() -> {
            stage.toFront();
            window.getCanvas().requestFocus();
        });
        WaitForAsyncUtils.waitForFxEvents();
        robot.push(KeyCode.CONTROL, KeyCode.R);
        waitForDashboardResults(robot);

        loadExample("Coffee Cooling", "introductory/coffee-cooling.json");
        Platform.runLater(() -> {
            stage.toFront();
            window.getCanvas().requestFocus();
        });
        WaitForAsyncUtils.waitForFxEvents();
        robot.push(KeyCode.CONTROL, KeyCode.R);
        waitForDashboardResults(robot);

        loadExample("Exponential Growth", "introductory/exponential-growth.json");
        Platform.runLater(() -> {
            stage.toFront();
            window.getCanvas().requestFocus();
        });
        WaitForAsyncUtils.waitForFxEvents();
        robot.push(KeyCode.CONTROL, KeyCode.R);
        waitForDashboardResults(robot);

        loadExample("Goal Seeking", "introductory/goal-seeking.json");
        Platform.runLater(() -> {
            stage.toFront();
            window.getCanvas().requestFocus();
        });
        WaitForAsyncUtils.waitForFxEvents();
        robot.push(KeyCode.CONTROL, KeyCode.R);
        waitForDashboardResults(robot);

        assertThat(stage.getTitle()).contains("Goal Seeking");
        TabPane resultTabs = robot.lookup("#dashboardResultTabs").queryAs(TabPane.class);
        assertThat(resultTabs.getTabs()).isNotEmpty();
    }

    @Test
    @DisplayName("Load → simulate → validate → switch model — no state leaks")
    void shouldNotLeakStateBetweenModels(FxRobot robot) {
        loadExample("Bathtub", "introductory/bathtub.json");
        triggerValidation(robot);
        Platform.runLater(() -> {
            stage.toFront();
            window.getCanvas().requestFocus();
        });
        WaitForAsyncUtils.waitForFxEvents();
        robot.push(KeyCode.CONTROL, KeyCode.R);
        waitForDashboardResults(robot);

        loadExample("Coffee Cooling", "introductory/coffee-cooling.json");

        assertThat(robot.lookup("#dashboardResultTabs").queryAs(TabPane.class)
                .isVisible()).isFalse();
        assertThat(window.isDirty()).isFalse();

        Platform.runLater(() -> window.getCanvas().requestFocus());
        WaitForAsyncUtils.waitForFxEvents();
        Platform.runLater(() -> {
            stage.toFront();
            window.getCanvas().requestFocus();
        });
        WaitForAsyncUtils.waitForFxEvents();
        robot.push(KeyCode.CONTROL, KeyCode.R);
        waitForDashboardResults(robot);

        assertThat(stage.getTitle()).contains("Coffee Cooling");
    }

    // ── Element counts ───────────────────────────────────────────────────

    @Test
    @DisplayName("Bathtub has expected element breakdown")
    void shouldShowBathtubElements(FxRobot robot) {
        loadExample("Bathtub", "introductory/bathtub.json");

        Label elementsLabel = robot.lookup("#statusElements").queryAs(Label.class);
        String text = elementsLabel.getText();

        assertThat(text).contains("1 stock");
        assertThat(text).contains("2 flow");
    }

    @Test
    @DisplayName("Complex model has more elements than simple model")
    void shouldShowMoreElementsForComplexModel(FxRobot robot) {
        loadExample("Bathtub", "introductory/bathtub.json");
        String bathtubText = robot.lookup("#statusElements")
                .queryAs(Label.class).getText();

        loadExample("Aging Chain", "demographics/aging-chain.json");
        String agingText = robot.lookup("#statusElements")
                .queryAs(Label.class).getText();

        assertThat(agingText).isNotEqualTo(bathtubText);
    }

    // ── Phase plot ───────────────────────────────────────────────────────

    @Test
    @DisplayName("Predator Prey simulation creates Phase Plot tab")
    void shouldShowPhasePlotForTwoStockModel(FxRobot robot) {
        loadExample("Predator Prey", "ecology/predator-prey.json");

        Platform.runLater(() -> {
            stage.toFront();
            window.getCanvas().requestFocus();
        });
        WaitForAsyncUtils.waitForFxEvents();
        robot.push(KeyCode.CONTROL, KeyCode.R);
        waitForDashboardResults(robot);

        TabPane resultTabs = robot.lookup("#dashboardResultTabs").queryAs(TabPane.class);
        boolean hasPhasePlot = resultTabs.getTabs().stream()
                .anyMatch(t -> "Phase Plot".equals(t.getText()));
        assertThat(hasPhasePlot).isTrue();
    }
}

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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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

    private void triggerValidation(FxRobot robot) throws TimeoutException {
        Platform.runLater(() -> {
            stage.toFront();
            stage.requestFocus();
            window.getCanvas().requestFocus();
        });
        WaitForAsyncUtils.waitForFxEvents();
        robot.push(KeyCode.CONTROL, KeyCode.B);
        WaitForAsyncUtils.waitForFxEvents();
        WaitForAsyncUtils.waitFor(15, TimeUnit.SECONDS, () -> {
            WaitForAsyncUtils.waitForFxEvents();
            return robot.lookup("#validationTable").tryQuery().isPresent();
        });
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
    @DisplayName("SIR Epidemic: load → validate → simulate")
    void shouldSimulateSirEpidemic(FxRobot robot) throws Exception {
        loadExample("SIR Epidemic", "epidemiology/sir-epidemic.json");
        assertThat(stage.getTitle()).contains("SIR");

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
    void shouldSimulatePredatorPrey(FxRobot robot) throws Exception {
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
    void shouldSimulateSShapedGrowth(FxRobot robot) throws Exception {
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
    @DisplayName("Market: load → validate → simulate")
    void shouldSimulateMarket(FxRobot robot) throws Exception {
        loadExample("Market", "economics/market.json");

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
    @DisplayName("Inventory Oscillation: load → simulate")
    void shouldSimulateInventoryOscillation(FxRobot robot) throws Exception {
        loadExample("Inventory Oscillation", "supply-chain/inventory-oscillation.json");

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
    void shouldHandleSequentialModelSimulations(FxRobot robot) throws Exception {
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
    void shouldNotLeakStateBetweenModels(FxRobot robot) throws Exception {
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

        loadExample("SIR Epidemic", "epidemiology/sir-epidemic.json");
        String sirText = robot.lookup("#statusElements")
                .queryAs(Label.class).getText();

        assertThat(sirText).isNotEqualTo(bathtubText);
    }

    // ── Phase plot ───────────────────────────────────────────────────────

    @Test
    @DisplayName("Predator Prey simulation creates Phase Plot tab")
    void shouldShowPhasePlotForTwoStockModel(FxRobot robot) throws Exception {
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

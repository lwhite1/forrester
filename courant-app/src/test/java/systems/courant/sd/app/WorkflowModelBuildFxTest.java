package systems.courant.sd.app;

import systems.courant.sd.app.canvas.Clipboard;
import systems.courant.sd.app.canvas.ModelEditor;
import systems.courant.sd.model.def.SimulationSettings;

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
 * End-to-end workflow tests for building models from scratch and simulating them.
 * Uses programmatic ModelEditor calls to construct models, then exercises the
 * full simulation pipeline via keyboard shortcuts.
 */
@DisplayName("Workflow: Model Building (TestFX)")
@ExtendWith(ApplicationExtension.class)
class WorkflowModelBuildFxTest {

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
     * Initializes the editor by creating a new empty model.
     * Must be called before accessing window.getEditor() in build tests.
     */
    private void initEditor() {
        Platform.runLater(() -> window.getFileController().newModel());
        WaitForAsyncUtils.waitForFxEvents();
    }

    private void focusCanvasAndSimulate(FxRobot robot) {
        Platform.runLater(() -> window.getCanvas().requestFocus());
        WaitForAsyncUtils.waitForFxEvents();
        robot.push(KeyCode.CONTROL, KeyCode.R);
    }

    private void waitForDashboardResults(FxRobot robot) throws TimeoutException {
        WaitForAsyncUtils.waitFor(30, TimeUnit.SECONDS, () -> {
            WaitForAsyncUtils.waitForFxEvents();
            var tabs = robot.lookup("#dashboardResultTabs").tryQueryAs(TabPane.class);
            return tabs.isPresent() && tabs.get().isVisible() && !tabs.get().getTabs().isEmpty();
        });
    }

    // ── Simple model builds ──────────────────────────────────────────────

    @Test
    @DisplayName("Single stock with constant drain: build → configure → simulate")
    void shouldBuildAndSimulateDrainModel(FxRobot robot) throws Exception {
        initEditor();
        ModelEditor editor = window.getEditor();

        Platform.runLater(() -> {
            editor.addStock();
            editor.setStockInitialValue("Stock 1", 100);
            editor.addFlow("Stock 1", null);
            editor.setFlowEquation("Flow 1", "5");
            editor.setSimulationSettings(new SimulationSettings("Day", 10, "Day"));
        });
        WaitForAsyncUtils.waitForFxEvents();

        // Elements added programmatically — verify editor state directly
        assertThat(window.getEditor().getStocks()).hasSize(1);
        assertThat(window.getEditor().getFlows()).hasSize(1);

        focusCanvasAndSimulate(robot);
        waitForDashboardResults(robot);

        TabPane resultTabs = robot.lookup("#dashboardResultTabs").queryAs(TabPane.class);
        assertThat(resultTabs.getTabs()).isNotEmpty();
        assertThat(resultTabs.getTabs().getFirst().getText()).isEqualTo("Simulation");
    }

    @Test
    @DisplayName("Stock with inflow and outflow: build → simulate → verify dirty")
    void shouldBuildBathtubFromScratch(FxRobot robot) throws Exception {
        initEditor();
        ModelEditor editor = window.getEditor();

        Platform.runLater(() -> {
            editor.addStock();
            editor.setStockInitialValue("Stock 1", 50);
            editor.addFlow(null, "Stock 1");
            editor.setFlowEquation("Flow 1", "3");
            editor.addFlow("Stock 1", null);
            editor.setFlowEquation("Flow 2", "5");
            editor.setSimulationSettings(new SimulationSettings("Day", 20, "Day"));
        });
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(window.isDirty()).isTrue();

        focusCanvasAndSimulate(robot);
        waitForDashboardResults(robot);

        TabPane resultTabs = robot.lookup("#dashboardResultTabs").queryAs(TabPane.class);
        assertThat(resultTabs.getTabs()).isNotEmpty();
    }

    @Test
    @DisplayName("Two interacting stocks: build → simulate")
    void shouldBuildTwoStockModel(FxRobot robot) throws Exception {
        initEditor();
        ModelEditor editor = window.getEditor();

        Platform.runLater(() -> {
            editor.addStock();
            editor.addStock();
            editor.setStockInitialValue("Stock 1", 100);
            editor.setStockInitialValue("Stock 2", 0);
            editor.addFlow("Stock 1", "Stock 2");
            editor.setFlowEquation("Flow 1", "Stock_1 * 0.1");
            editor.setSimulationSettings(new SimulationSettings("Day", 50, "Day"));
        });
        WaitForAsyncUtils.waitForFxEvents();

        focusCanvasAndSimulate(robot);
        waitForDashboardResults(robot);

        TabPane resultTabs = robot.lookup("#dashboardResultTabs").queryAs(TabPane.class);
        assertThat(resultTabs.getTabs()).isNotEmpty();
    }

    // ── Models with auxiliaries ──────────────────────────────────────────

    @Test
    @DisplayName("Stock + auxiliary + flow referencing auxiliary")
    void shouldBuildModelWithAuxiliary(FxRobot robot) throws Exception {
        initEditor();
        ModelEditor editor = window.getEditor();

        Platform.runLater(() -> {
            editor.addStock();
            editor.setStockInitialValue("Stock 1", 100);
            editor.addVariable();
            editor.setVariableEquation("Variable 1", "Stock_1 * 0.05");
            editor.addFlow("Stock 1", null);
            editor.setFlowEquation("Flow 1", "Variable_1");
            editor.setSimulationSettings(new SimulationSettings("Day", 30, "Day"));
        });
        WaitForAsyncUtils.waitForFxEvents();

        focusCanvasAndSimulate(robot);
        waitForDashboardResults(robot);

        TabPane resultTabs = robot.lookup("#dashboardResultTabs").queryAs(TabPane.class);
        assertThat(resultTabs.getTabs()).isNotEmpty();
    }

    @Test
    @DisplayName("Exponential growth model with renamed elements")
    void shouldBuildExponentialGrowthModel(FxRobot robot) throws Exception {
        initEditor();
        ModelEditor editor = window.getEditor();

        Platform.runLater(() -> {
            editor.setModelName("My Exponential Growth");
            editor.addStock();
            editor.renameElement("Stock 1", "Population");
            editor.setStockInitialValue("Population", 10);
            editor.addVariable();
            editor.renameElement("Variable 1", "Growth Rate");
            editor.setVariableEquation("Growth Rate", "0.1");
            editor.addFlow(null, "Population");
            editor.renameElement("Flow 1", "Births");
            editor.setFlowEquation("Births", "Population * Growth_Rate");
            editor.setSimulationSettings(new SimulationSettings("Year", 50, "Year"));
        });
        WaitForAsyncUtils.waitForFxEvents();

        // Title is updated on model load, not on setModelName — verify editor directly
        assertThat(window.getEditor().getModelName()).isEqualTo("My Exponential Growth");

        focusCanvasAndSimulate(robot);
        waitForDashboardResults(robot);

        TabPane resultTabs = robot.lookup("#dashboardResultTabs").queryAs(TabPane.class);
        assertThat(resultTabs.getTabs()).isNotEmpty();
    }

    // ── Iterative build-simulate-refine cycle ────────────────────────────

    @Test
    @DisplayName("Build → simulate → add element → simulate again")
    void shouldSupportIterativeRefinement(FxRobot robot) throws Exception {
        initEditor();
        ModelEditor editor = window.getEditor();

        Platform.runLater(() -> {
            editor.addStock();
            editor.setStockInitialValue("Stock 1", 100);
            editor.addFlow("Stock 1", null);
            editor.setFlowEquation("Flow 1", "10");
            editor.setSimulationSettings(new SimulationSettings("Day", 10, "Day"));
        });
        WaitForAsyncUtils.waitForFxEvents();

        focusCanvasAndSimulate(robot);
        waitForDashboardResults(robot);

        Platform.runLater(() -> {
            editor.addFlow(null, "Stock 1");
            editor.setFlowEquation("Flow 2", "5");
        });
        WaitForAsyncUtils.waitForFxEvents();

        var banner = robot.lookup("#staleBanner")
                .queryAs(javafx.scene.layout.HBox.class);
        assertThat(banner.isVisible()).isTrue();

        focusCanvasAndSimulate(robot);
        waitForDashboardResults(robot);

        banner = robot.lookup("#staleBanner")
                .queryAs(javafx.scene.layout.HBox.class);
        assertThat(banner.isVisible()).isFalse();
    }

    @Test
    @DisplayName("Build → simulate → rename element → simulate again")
    void shouldSimulateAfterRenaming(FxRobot robot) throws Exception {
        initEditor();
        ModelEditor editor = window.getEditor();

        Platform.runLater(() -> {
            editor.addStock();
            editor.setStockInitialValue("Stock 1", 50);
            editor.addFlow("Stock 1", null);
            editor.setFlowEquation("Flow 1", "5");
            editor.setSimulationSettings(new SimulationSettings("Day", 10, "Day"));
        });
        WaitForAsyncUtils.waitForFxEvents();

        focusCanvasAndSimulate(robot);
        waitForDashboardResults(robot);

        Platform.runLater(() -> {
            editor.renameElement("Stock 1", "Water");
            editor.setFlowEquation("Flow 1", "5");
        });
        WaitForAsyncUtils.waitForFxEvents();

        focusCanvasAndSimulate(robot);
        waitForDashboardResults(robot);

        TabPane resultTabs = robot.lookup("#dashboardResultTabs").queryAs(TabPane.class);
        assertThat(resultTabs.getTabs()).isNotEmpty();
    }

    @Test
    @DisplayName("Build → simulate → change equation → simulate → verify ghost runs")
    void shouldTrackGhostRunsAcrossChanges(FxRobot robot) throws Exception {
        initEditor();
        ModelEditor editor = window.getEditor();

        Platform.runLater(() -> {
            editor.addStock();
            editor.setStockInitialValue("Stock 1", 100);
            editor.addFlow("Stock 1", null);
            editor.setFlowEquation("Flow 1", "5");
            editor.setSimulationSettings(new SimulationSettings("Day", 10, "Day"));
        });
        WaitForAsyncUtils.waitForFxEvents();

        focusCanvasAndSimulate(robot);
        waitForDashboardResults(robot);

        Platform.runLater(() -> editor.setFlowEquation("Flow 1", "10"));
        WaitForAsyncUtils.waitForFxEvents();

        focusCanvasAndSimulate(robot);
        waitForDashboardResults(robot);

        var ghostHeader = robot.lookup("#ghostRunsHeader").tryQueryAs(Label.class);
        assertThat(ghostHeader).isPresent();
    }

    // ── Validation of hand-built models ──────────────────────────────────

    @Test
    @DisplayName("Valid hand-built model passes validation")
    void shouldValidateCorrectModel(FxRobot robot) throws Exception {
        initEditor();
        ModelEditor editor = window.getEditor();

        Platform.runLater(() -> {
            editor.addStock();
            editor.setStockInitialValue("Stock 1", 100);
            editor.addFlow("Stock 1", null);
            editor.setFlowEquation("Flow 1", "5");
            editor.setSimulationSettings(new SimulationSettings("Day", 10, "Day"));
        });
        WaitForAsyncUtils.waitForFxEvents();

        robot.push(KeyCode.CONTROL, KeyCode.B);
        WaitForAsyncUtils.waitForFxEvents();
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS, () -> {
            WaitForAsyncUtils.waitForFxEvents();
            return robot.lookup("#validationTable").tryQuery().isPresent();
        });
        Platform.runLater(() -> window.getCanvas().requestFocus());
        WaitForAsyncUtils.waitForFxEvents();

        Label validationLabel = robot.lookup("#statusValidation").queryAs(Label.class);
        assertThat(validationLabel.getText()).contains("No issues");
    }

    @Test
    @DisplayName("Model with undefined reference shows validation errors")
    void shouldDetectUndefinedReference(FxRobot robot) throws Exception {
        initEditor();
        ModelEditor editor = window.getEditor();

        Platform.runLater(() -> {
            editor.addStock();
            editor.setStockInitialValue("Stock 1", 100);
            editor.addFlow("Stock 1", null);
            // Reference a variable that doesn't exist
            editor.setFlowEquation("Flow 1", "nonexistent_variable");
            editor.setSimulationSettings(new SimulationSettings("Day", 10, "Day"));
        });
        WaitForAsyncUtils.waitForFxEvents();

        Platform.runLater(() -> window.getCanvas().requestFocus());
        WaitForAsyncUtils.waitForFxEvents();
        robot.push(KeyCode.CONTROL, KeyCode.B);
        WaitForAsyncUtils.waitForFxEvents();
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS, () -> {
            WaitForAsyncUtils.waitForFxEvents();
            return robot.lookup("#validationTable").tryQuery().isPresent();
        });
        Platform.runLater(() -> window.getCanvas().requestFocus());
        WaitForAsyncUtils.waitForFxEvents();

        Label validationLabel = robot.lookup("#statusValidation").queryAs(Label.class);
        assertThat(validationLabel.getText()).doesNotContain("No issues");
    }
}

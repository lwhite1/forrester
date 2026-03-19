package systems.courant.sd.app;

import systems.courant.sd.app.canvas.Clipboard;
import systems.courant.sd.app.canvas.ModelEditor;
import systems.courant.sd.model.def.SimulationSettings;

import javafx.application.Platform;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DialogPane;
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

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end workflow tests for advanced analysis: parameter sweep, multi-parameter
 * sweep, Monte Carlo, and optimization. Tests build models with tunable parameters
 * and exercise the analysis pipeline via menu interactions.
 */
@DisplayName("Workflow: Sweep & Analysis (TestFX)")
@ExtendWith(ApplicationExtension.class)
class WorkflowSweepAnalysisFxTest {

    private ModelWindow window;
    private Stage stage;

    @Start
    void start(Stage stage) {
        this.stage = stage;
        CourantApp app = new CourantApp();
        window = new ModelWindow(stage, app, new Clipboard());
        stage.show();
    }

    private void initEditor() {
        Platform.runLater(() -> window.getFileController().newModel());
        WaitForAsyncUtils.waitForFxEvents();
    }

    /**
     * Builds a model with one tunable parameter: Stock 1 drains at Drain Rate.
     */
    private void buildSweepableModel() {
        initEditor();
        ModelEditor editor = window.getEditor();
        Platform.runLater(() -> {
            editor.addStock();
            editor.setStockInitialValue("Stock 1", 100);
            editor.addVariable();
            editor.renameElement("Variable 1", "Drain Rate");
            editor.setVariableEquation("Drain Rate", "5");
            editor.addFlow("Stock 1", null);
            editor.setFlowEquation("Flow 1", "Drain_Rate");
            editor.setSimulationSettings(new SimulationSettings("Day", 20, "Day"));
        });
        WaitForAsyncUtils.waitForFxEvents();
    }

    /**
     * Builds a model with two tunable parameters.
     */
    private void buildTwoParameterModel() {
        initEditor();
        ModelEditor editor = window.getEditor();
        Platform.runLater(() -> {
            editor.addStock();
            editor.setStockInitialValue("Stock 1", 100);
            editor.addVariable();
            editor.renameElement("Variable 1", "Drain Rate");
            editor.setVariableEquation("Drain Rate", "5");
            editor.addVariable();
            editor.renameElement("Variable 2", "Fill Rate");
            editor.setVariableEquation("Fill Rate", "3");
            editor.addFlow("Stock 1", null);
            editor.setFlowEquation("Flow 1", "Drain_Rate");
            editor.addFlow(null, "Stock 1");
            editor.setFlowEquation("Flow 2", "Fill_Rate");
            editor.setSimulationSettings(new SimulationSettings("Day", 20, "Day"));
        });
        WaitForAsyncUtils.waitForFxEvents();
    }

    private void waitForDashboardResults(FxRobot robot) throws TimeoutException {
        WaitForAsyncUtils.waitFor(15, TimeUnit.SECONDS, () -> {
            WaitForAsyncUtils.waitForFxEvents();
            var tabs = robot.lookup("#dashboardResultTabs").tryQueryAs(TabPane.class);
            return tabs.isPresent() && tabs.get().isVisible() && !tabs.get().getTabs().isEmpty();
        });
    }

    private void waitForDashboardTab(FxRobot robot, String tabName) throws TimeoutException {
        WaitForAsyncUtils.waitFor(15, TimeUnit.SECONDS, () -> {
            WaitForAsyncUtils.waitForFxEvents();
            var tabs = robot.lookup("#dashboardResultTabs").tryQueryAs(TabPane.class);
            return tabs.isPresent() && tabs.get().isVisible()
                    && tabs.get().getTabs().stream().anyMatch(t -> tabName.equals(t.getText()));
        });
    }

    // ── Parameter identification ─────────────────────────────────────────

    @Test
    @DisplayName("Single-parameter model identifies Drain Rate as sweepable")
    void shouldIdentifyParameters(FxRobot robot) {
        buildSweepableModel();

        List<String> params = window.getEditor().getParameterNames();
        assertThat(params).contains("Drain Rate");
    }

    @Test
    @DisplayName("Two-parameter model identifies both parameters")
    void shouldIdentifyMultipleParameters(FxRobot robot) {
        buildTwoParameterModel();

        List<String> params = window.getEditor().getParameterNames();
        assertThat(params).containsExactlyInAnyOrder("Drain Rate", "Fill Rate");
    }

    // ── Parameter Sweep via menu ─────────────────────────────────────────

    @Test
    @DisplayName("Open sweep dialog via menu — dialog has parameter combo with model params")
    @SuppressWarnings("unchecked")
    void shouldOpenParameterSweepViaMenu(FxRobot robot) {
        buildSweepableModel();

        robot.clickOn("Simulate");
        WaitForAsyncUtils.waitForFxEvents();
        robot.clickOn("Parameter Sweep...");
        WaitForAsyncUtils.waitForFxEvents();

        // Verify dialog is open with the model's parameter available
        Set<ComboBox> combos = robot.lookup(".combo-box").queryAllAs(ComboBox.class);
        boolean hasDrainRate = combos.stream()
                .anyMatch(c -> c.getItems().contains("Drain Rate"));
        assertThat(hasDrainRate)
                .as("Sweep dialog should show 'Drain Rate' parameter")
                .isTrue();

        robot.clickOn("Cancel");
        WaitForAsyncUtils.waitForFxEvents();
    }

    // ── Combined simulation + sweep ──────────────────────────────────────

    @Test
    @DisplayName("Simulate model, then open sweep dialog — both workflows accessible")
    void shouldAccessBothSimAndSweepWorkflows(FxRobot robot) throws Exception {
        buildSweepableModel();

        // Run simulation
        Platform.runLater(() -> {
            stage.toFront();
            window.getCanvas().requestFocus();
        });
        WaitForAsyncUtils.waitForFxEvents();
        robot.push(KeyCode.CONTROL, KeyCode.R);
        waitForDashboardResults(robot);

        TabPane resultTabs = robot.lookup("#dashboardResultTabs").queryAs(TabPane.class);
        assertThat(resultTabs.getTabs().stream().map(t -> t.getText()).toList())
                .contains("Simulation");

        // Open sweep dialog (available after simulation)
        robot.clickOn("Simulate");
        WaitForAsyncUtils.waitForFxEvents();
        robot.clickOn("Parameter Sweep...");
        WaitForAsyncUtils.waitForFxEvents();

        // Verify dialog shows parameters
        Set<ComboBox> combos = robot.lookup(".combo-box").queryAllAs(ComboBox.class);
        boolean hasDrainRate = combos.stream()
                .anyMatch(c -> c.getItems().contains("Drain Rate"));
        assertThat(hasDrainRate).isTrue();

        robot.clickOn("Cancel");
        WaitForAsyncUtils.waitForFxEvents();
    }

    // ── Monte Carlo dialog ───────────────────────────────────────────────

    @Test
    @DisplayName("Open Monte Carlo dialog for model with parameters")
    void shouldOpenMonteCarloDialog(FxRobot robot) {
        buildSweepableModel();

        robot.clickOn("Simulate");
        WaitForAsyncUtils.waitForFxEvents();
        robot.clickOn("Monte Carlo...");
        WaitForAsyncUtils.waitForFxEvents();

        Set<TextField> fields = robot.lookup(".text-field").queryAllAs(TextField.class);
        assertThat(fields).isNotEmpty();

        robot.clickOn("Cancel");
        WaitForAsyncUtils.waitForFxEvents();
    }

    // ── Optimization dialog ──────────────────────────────────────────────

    @Test
    @DisplayName("Open Optimization dialog for model with parameters and stocks")
    void shouldOpenOptimizerDialog(FxRobot robot) {
        buildSweepableModel();

        robot.clickOn("Simulate");
        WaitForAsyncUtils.waitForFxEvents();
        robot.clickOn("Optimize...");
        WaitForAsyncUtils.waitForFxEvents();

        Set<ComboBox> combos = robot.lookup(".combo-box").queryAllAs(ComboBox.class);
        assertThat(combos).isNotEmpty();

        robot.clickOn("Cancel");
        WaitForAsyncUtils.waitForFxEvents();
    }

    // ── Multi-parameter sweep ────────────────────────────────────────────

    @Test
    @DisplayName("Open multi-sweep dialog for two-parameter model")
    void shouldOpenMultiSweepForTwoParamModel(FxRobot robot) {
        buildTwoParameterModel();

        robot.clickOn("Simulate");
        WaitForAsyncUtils.waitForFxEvents();
        robot.clickOn("Multi-Parameter Sweep...");
        WaitForAsyncUtils.waitForFxEvents();

        var addButton = robot.lookup("#multiSweepAddParam").tryQuery();
        assertThat(addButton).isPresent();

        robot.clickOn("Cancel");
        WaitForAsyncUtils.waitForFxEvents();
    }

    // ── Error paths ──────────────────────────────────────────────────────

    @Test
    @DisplayName("Sweep shows error when model has no parameters")
    void shouldShowErrorForNoParameters(FxRobot robot) {
        initEditor();
        ModelEditor editor = window.getEditor();
        Platform.runLater(() -> {
            editor.addStock();
            editor.setStockInitialValue("Stock 1", 100);
            editor.addFlow("Stock 1", null);
            editor.setFlowEquation("Flow 1", "Stock_1 * 0.1");
            editor.setSimulationSettings(new SimulationSettings("Day", 10, "Day"));
        });
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(editor.getParameterNames()).isEmpty();

        robot.clickOn("Simulate");
        WaitForAsyncUtils.waitForFxEvents();
        robot.clickOn("Parameter Sweep...");
        WaitForAsyncUtils.waitForFxEvents();

        // Error alert should appear — verify and dismiss it
        var errorDialog = robot.lookup(".dialog-pane").tryQueryAs(DialogPane.class);
        assertThat(errorDialog)
                .as("Error dialog should appear when model has no parameters")
                .isPresent();
        assertThat(errorDialog.get().getContentText())
                .contains("no parameters");

        robot.clickOn("OK");
        WaitForAsyncUtils.waitForFxEvents();
    }

    @Test
    @DisplayName("Multi-sweep shows error for single-parameter model")
    void shouldShowErrorForSingleParameter(FxRobot robot) {
        buildSweepableModel();

        assertThat(window.getEditor().getParameterNames()).hasSize(1);

        robot.clickOn("Simulate");
        WaitForAsyncUtils.waitForFxEvents();
        robot.clickOn("Multi-Parameter Sweep...");
        WaitForAsyncUtils.waitForFxEvents();

        // Error alert should appear — verify and dismiss it
        var errorDialog = robot.lookup(".dialog-pane").tryQueryAs(DialogPane.class);
        assertThat(errorDialog)
                .as("Error dialog should appear when model has fewer than 2 parameters")
                .isPresent();
        assertThat(errorDialog.get().getContentText())
                .contains("at least 2 parameters");

        robot.clickOn("OK");
        WaitForAsyncUtils.waitForFxEvents();
    }

    // ── Full analysis workflow ────────────────────────────────────────────

    @Test
    @DisplayName("Build → validate → simulate → modify → re-simulate (full cycle)")
    void shouldCompleteFullAnalysisCycle(FxRobot robot) throws Exception {
        buildSweepableModel();

        // Validate
        Platform.runLater(() -> {
            stage.toFront();
            window.getCanvas().requestFocus();
        });
        WaitForAsyncUtils.waitForFxEvents();
        robot.push(KeyCode.CONTROL, KeyCode.B);
        WaitForAsyncUtils.waitForFxEvents();
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS, () -> {
            WaitForAsyncUtils.waitForFxEvents();
            return robot.lookup("#validationTable").tryQuery().isPresent();
        });

        Label validationLabel = robot.lookup("#statusValidation").queryAs(Label.class);
        assertThat(validationLabel.getText()).contains("No issues");

        // Simulate (restore focus after validation dialog)
        Platform.runLater(() -> {
            stage.toFront();
            stage.requestFocus();
            window.getCanvas().requestFocus();
        });
        WaitForAsyncUtils.waitForFxEvents();
        robot.push(KeyCode.CONTROL, KeyCode.R);
        waitForDashboardResults(robot);

        // Modify parameter
        Platform.runLater(() -> window.getEditor().setVariableEquation("Drain Rate", "8"));
        WaitForAsyncUtils.waitForFxEvents();

        // Re-simulate
        Platform.runLater(() -> {
            stage.toFront();
            stage.requestFocus();
            window.getCanvas().requestFocus();
        });
        WaitForAsyncUtils.waitForFxEvents();
        robot.push(KeyCode.CONTROL, KeyCode.R);
        waitForDashboardResults(robot);

        var ghostHeader = robot.lookup("#ghostRunsHeader").tryQueryAs(Label.class);
        assertThat(ghostHeader).isPresent();
    }
}

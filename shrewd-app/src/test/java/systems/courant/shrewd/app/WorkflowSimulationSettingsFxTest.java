package systems.courant.shrewd.app;

import systems.courant.shrewd.app.canvas.Clipboard;
import systems.courant.shrewd.app.canvas.ModelEditor;
import systems.courant.shrewd.model.def.SimulationSettings;

import javafx.application.Platform;
import javafx.scene.control.ComboBox;
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end workflow tests for simulation settings: changing time step,
 * duration, and duration unit via the settings dialog, then simulating.
 */
@DisplayName("Workflow: Simulation Settings (TestFX)")
@ExtendWith(ApplicationExtension.class)
class WorkflowSimulationSettingsFxTest {

    private ModelWindow window;
    private Stage stage;

    @Start
    void start(Stage stage) {
        this.stage = stage;
        ShrewdApp app = new ShrewdApp();
        window = new ModelWindow(stage, app, new Clipboard());
        stage.show();
    }

    private void loadExample(String name, String resourcePath) {
        Platform.runLater(() ->
                window.getFileController().openExample(name, resourcePath));
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

    // ── Settings dialog via menu ─────────────────────────────────────────

    @Test
    @DisplayName("Settings dialog shows Bathtub default values (Minute, 10, Minute)")
    void shouldShowSettingsFromExampleModel(FxRobot robot) {
        loadExample("Bathtub", "introductory/bathtub.json");

        robot.clickOn("Simulate");
        WaitForAsyncUtils.waitForFxEvents();
        robot.clickOn("Simulation Settings...");
        WaitForAsyncUtils.waitForFxEvents();

        ComboBox<?> timeStep = robot.lookup("#simTimeStep").queryAs(ComboBox.class);
        TextField duration = robot.lookup("#simDuration").queryAs(TextField.class);
        ComboBox<?> durationUnit = robot.lookup("#simDurationUnit").queryAs(ComboBox.class);

        assertThat(timeStep.getValue()).isEqualTo("Minute");
        assertThat(duration.getText()).isEqualTo("10");
        assertThat(durationUnit.getValue()).isEqualTo("Minute");

        robot.clickOn("Cancel");
        WaitForAsyncUtils.waitForFxEvents();
    }

    @Test
    @DisplayName("Change duration in settings, then simulate successfully")
    void shouldApplyChangedDuration(FxRobot robot) {
        loadExample("Bathtub", "introductory/bathtub.json");

        robot.clickOn("Simulate");
        WaitForAsyncUtils.waitForFxEvents();
        robot.clickOn("Simulation Settings...");
        WaitForAsyncUtils.waitForFxEvents();

        TextField duration = robot.lookup("#simDuration").queryAs(TextField.class);
        robot.clickOn(duration).eraseText(duration.getText().length()).write("20");
        WaitForAsyncUtils.waitForFxEvents();

        robot.clickOn("OK");
        WaitForAsyncUtils.waitForFxEvents();

        SimulationSettings settings = window.getEditor().getSimulationSettings();
        assertThat(settings.duration()).isEqualTo(20.0);

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

    @Test
    @DisplayName("Change time step unit in settings dialog")
    @SuppressWarnings("unchecked")
    void shouldApplyChangedTimeStep(FxRobot robot) {
        loadExample("Bathtub", "introductory/bathtub.json");

        robot.clickOn("Simulate");
        WaitForAsyncUtils.waitForFxEvents();
        robot.clickOn("Simulation Settings...");
        WaitForAsyncUtils.waitForFxEvents();

        ComboBox<String> timeStep = robot.lookup("#simTimeStep").queryAs(ComboBox.class);
        Platform.runLater(() -> timeStep.getSelectionModel().select("Second"));
        WaitForAsyncUtils.waitForFxEvents();

        robot.clickOn("OK");
        WaitForAsyncUtils.waitForFxEvents();

        SimulationSettings settings = window.getEditor().getSimulationSettings();
        assertThat(settings.timeStep()).isEqualTo("Second");
    }

    // ── Settings persistence ─────────────────────────────────────────────

    @Test
    @DisplayName("Programmatically set settings persist when dialog is reopened")
    void shouldPersistSettingsAcrossDialogOpens(FxRobot robot) {
        loadExample("Bathtub", "introductory/bathtub.json");

        Platform.runLater(() ->
                window.getEditor().setSimulationSettings(
                        new SimulationSettings("Hour", 48, "Hour")));
        WaitForAsyncUtils.waitForFxEvents();

        robot.clickOn("Simulate");
        WaitForAsyncUtils.waitForFxEvents();
        robot.clickOn("Simulation Settings...");
        WaitForAsyncUtils.waitForFxEvents();

        ComboBox<?> timeStep = robot.lookup("#simTimeStep").queryAs(ComboBox.class);
        TextField duration = robot.lookup("#simDuration").queryAs(TextField.class);
        ComboBox<?> durationUnit = robot.lookup("#simDurationUnit").queryAs(ComboBox.class);

        assertThat(timeStep.getValue()).isEqualTo("Hour");
        assertThat(duration.getText()).isEqualTo("48");
        assertThat(durationUnit.getValue()).isEqualTo("Hour");

        robot.clickOn("Cancel");
        WaitForAsyncUtils.waitForFxEvents();
    }

    @Test
    @DisplayName("Loading new example resets settings to model defaults")
    void shouldResetSettingsOnNewExample(FxRobot robot) {
        loadExample("Bathtub", "introductory/bathtub.json");
        Platform.runLater(() ->
                window.getEditor().setSimulationSettings(
                        new SimulationSettings("Year", 100, "Year")));
        WaitForAsyncUtils.waitForFxEvents();

        loadExample("Coffee Cooling", "introductory/coffee-cooling.json");

        SimulationSettings settings = window.getEditor().getSimulationSettings();
        assertThat(settings).isNotNull();
        assertThat(settings.timeStep()).isNotEqualTo("Year");
    }

    // ── New model settings ───────────────────────────────────────────────

    @Test
    @DisplayName("New model has no simulation settings initially")
    void shouldHaveNoSettingsOnNewModel(FxRobot robot) {
        Platform.runLater(() -> window.getFileController().newModel());
        WaitForAsyncUtils.waitForFxEvents();

        SimulationSettings settings = window.getEditor().getSimulationSettings();
        assertThat(settings).isNull();
    }

    @Test
    @DisplayName("Setting settings programmatically then running succeeds")
    void shouldRunWithProgrammaticSettings(FxRobot robot) {
        Platform.runLater(() -> window.getFileController().newModel());
        WaitForAsyncUtils.waitForFxEvents();
        ModelEditor editor = window.getEditor();

        Platform.runLater(() -> {
            editor.addStock();
            editor.setStockInitialValue("Stock 1", 50);
            editor.addFlow("Stock 1", null);
            editor.setFlowEquation("Flow 1", "5");
            editor.setSimulationSettings(new SimulationSettings("Day", 10, "Day"));
        });
        WaitForAsyncUtils.waitForFxEvents();

        Platform.runLater(() -> window.getCanvas().requestFocus());
        WaitForAsyncUtils.waitForFxEvents();
        robot.push(KeyCode.CONTROL, KeyCode.R);
        waitForDashboardResults(robot);

        TabPane resultTabs = robot.lookup("#dashboardResultTabs").queryAs(TabPane.class);
        assertThat(resultTabs.getTabs()).isNotEmpty();
    }
}

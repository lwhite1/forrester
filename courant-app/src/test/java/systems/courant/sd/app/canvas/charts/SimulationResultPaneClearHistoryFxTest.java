package systems.courant.sd.app.canvas.charts;

import systems.courant.sd.model.def.FlowDef;
import systems.courant.sd.model.def.ReferenceDataset;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import systems.courant.sd.app.canvas.DashboardPanel;
import systems.courant.sd.app.canvas.SimulationRunner;

@DisplayName("SimulationResultPane Clear History (#425)")
@ExtendWith(ApplicationExtension.class)
class SimulationResultPaneClearHistoryFxTest {

    private DashboardPanel panel;

    private SimulationRunner.SimulationResult simulationResult() {
        return new SimulationRunner.SimulationResult(
                List.of("Step", "Population", "BirthRate"),
                List.of(
                        new double[]{0, 100, 10},
                        new double[]{1, 110, 11},
                        new double[]{2, 121, 12}
                ),
                Map.of("Population", "Person", "BirthRate", "Person/Day"),
                Set.of("Population")
        );
    }

    private List<FlowDef> flows() {
        return List.of(
                new FlowDef("BirthRate", "Population * 0.1", "Day", null, "Population")
        );
    }

    private ReferenceDataset referenceData() {
        return new ReferenceDataset(
                "Historical",
                new double[]{0, 1, 2},
                Map.of("Population", new double[]{100, 108, 117})
        );
    }

    @Start
    void start(Stage stage) {
        panel = new DashboardPanel();
        stage.setScene(new Scene(new StackPane(panel), 800, 600));
        stage.show();
    }

    private void switchToChartTab(FxRobot robot) {
        Platform.runLater(() -> {
            TabPane resultTabs = robot.lookup("#dashboardResultTabs").queryAs(TabPane.class);
            TabPane innerTabs = (TabPane) resultTabs.getTabs().getFirst().getContent()
                    .lookup(".tab-pane");
            if (innerTabs != null) {
                innerTabs.getSelectionModel().select(1); // Chart tab
            }
        });
        WaitForAsyncUtils.waitForFxEvents();
    }

    @Test
    @DisplayName("Clear History should preserve net flow toggle when ghost runs exist")
    void shouldPreserveNetFlowToggleAfterClearHistory(FxRobot robot) {
        // First run: creates a ghost entry for next run
        Platform.runLater(() ->
                panel.showSimulationResult(simulationResult(), Map.of(), flows(), List.of()));
        WaitForAsyncUtils.waitForFxEvents();

        // Second run: ghost run appears in sidebar alongside net flow toggle
        Platform.runLater(() ->
                panel.showSimulationResult(simulationResult(), Map.of(), flows(), List.of()));
        WaitForAsyncUtils.waitForFxEvents();
        switchToChartTab(robot);

        // Verify net flow toggle and ghost header both exist before clearing
        CheckBox netFlowsBefore = robot.lookup("#showNetFlows").queryAs(CheckBox.class);
        assertThat(netFlowsBefore).isNotNull();

        Label ghostHeader = robot.lookup("#ghostRunsHeader").queryAs(Label.class);
        assertThat(ghostHeader).isNotNull();

        // Click Clear History
        robot.clickOn("Clear History");
        WaitForAsyncUtils.waitForFxEvents();

        // Ghost header should be gone
        var ghostAfter = robot.lookup("#ghostRunsHeader").tryQueryAs(Label.class);
        assertThat(ghostAfter).isEmpty();

        // Net flow toggle should still be present
        CheckBox netFlowsAfter = robot.lookup("#showNetFlows").queryAs(CheckBox.class);
        assertThat(netFlowsAfter).isNotNull();
    }

    @Test
    @DisplayName("Clear History should preserve reference data section when ghost runs exist")
    void shouldPreserveReferenceDataAfterClearHistory(FxRobot robot) {
        // First run
        Platform.runLater(() ->
                panel.showSimulationResult(simulationResult(), Map.of(), flows(),
                        List.of(referenceData())));
        WaitForAsyncUtils.waitForFxEvents();

        // Second run: ghost run + reference data + net flow toggle all present
        Platform.runLater(() ->
                panel.showSimulationResult(simulationResult(), Map.of(), flows(),
                        List.of(referenceData())));
        WaitForAsyncUtils.waitForFxEvents();
        switchToChartTab(robot);

        // Verify reference data header exists before clearing
        Label refHeader = robot.lookup("#referenceDataHeader").queryAs(Label.class);
        assertThat(refHeader).isNotNull();

        // Click Clear History
        robot.clickOn("Clear History");
        WaitForAsyncUtils.waitForFxEvents();

        // Ghost header should be gone
        var ghostAfter = robot.lookup("#ghostRunsHeader").tryQueryAs(Label.class);
        assertThat(ghostAfter).isEmpty();

        // Reference data header should still be present
        Label refAfter = robot.lookup("#referenceDataHeader").queryAs(Label.class);
        assertThat(refAfter).isNotNull();
    }

    @Test
    @DisplayName("Clear History should preserve both net flows and reference data")
    void shouldPreserveBothNetFlowsAndReferenceDataAfterClearHistory(FxRobot robot) {
        // First run
        Platform.runLater(() ->
                panel.showSimulationResult(simulationResult(), Map.of(), flows(),
                        List.of(referenceData())));
        WaitForAsyncUtils.waitForFxEvents();

        // Second run
        Platform.runLater(() ->
                panel.showSimulationResult(simulationResult(), Map.of(), flows(),
                        List.of(referenceData())));
        WaitForAsyncUtils.waitForFxEvents();
        switchToChartTab(robot);

        // All three sections should exist
        assertThat(robot.lookup("#showNetFlows").tryQueryAs(CheckBox.class)).isPresent();
        assertThat(robot.lookup("#ghostRunsHeader").tryQueryAs(Label.class)).isPresent();
        assertThat(robot.lookup("#referenceDataHeader").tryQueryAs(Label.class)).isPresent();

        // Click Clear History
        robot.clickOn("Clear History");
        WaitForAsyncUtils.waitForFxEvents();

        // Ghost section removed, others preserved
        assertThat(robot.lookup("#ghostRunsHeader").tryQueryAs(Label.class)).isEmpty();
        assertThat(robot.lookup("#showNetFlows").tryQueryAs(CheckBox.class)).isPresent();
        assertThat(robot.lookup("#referenceDataHeader").tryQueryAs(Label.class)).isPresent();
    }
}

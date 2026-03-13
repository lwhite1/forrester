package systems.courant.sd.app.canvas;

import systems.courant.sd.model.def.ReferenceDataset;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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

@DisplayName("Reference Data Overlay (TestFX)")
@ExtendWith(ApplicationExtension.class)
class ReferenceDataOverlayFxTest {

    private DashboardPanel panel;

    private SimulationRunner.SimulationResult simulationResult() {
        return new SimulationRunner.SimulationResult(
                List.of("Step", "Population", "Revenue"),
                List.of(
                        new double[]{0, 100, 50},
                        new double[]{1, 112, 56},
                        new double[]{2, 118, 59},
                        new double[]{3, 122, 61}
                ),
                Map.of("Population", "people", "Revenue", "$"),
                Set.of("Population")
        );
    }

    private ReferenceDataset referenceData() {
        return new ReferenceDataset(
                "Historical",
                new double[]{0, 1, 2, 3},
                Map.of("Population", new double[]{100, 110, 115, 118})
        );
    }

    @Start
    void start(Stage stage) {
        panel = new DashboardPanel();
        stage.setScene(new Scene(new StackPane(panel), 800, 600));
        stage.show();
    }

    @Test
    @DisplayName("Reference data series appears on chart when dataset provided")
    void referenceDataAppearsOnChart(FxRobot robot) {
        Platform.runLater(() ->
                panel.showSimulationResult(simulationResult(), Map.of(), List.of(),
                        List.of(referenceData())));
        WaitForAsyncUtils.waitForFxEvents();

        // Switch to Chart tab
        Platform.runLater(() -> {
            TabPane resultTabs = robot.lookup("#dashboardResultTabs").queryAs(TabPane.class);
            TabPane innerTabs = (TabPane) resultTabs.getTabs().getFirst().getContent()
                    .lookup(".tab-pane");
            if (innerTabs != null) {
                innerTabs.getSelectionModel().select(1); // Chart tab
            }
        });
        WaitForAsyncUtils.waitForFxEvents();

        // Find the Reference Data header in the sidebar
        Label refHeader = robot.lookup("#referenceDataHeader").queryAs(Label.class);
        assertThat(refHeader).isNotNull();
        assertThat(refHeader.getText()).isEqualTo("Reference Data");
    }

    @Test
    @DisplayName("Reference series has correct name with (observed) suffix")
    void referenceSeriesHasObservedSuffix(FxRobot robot) {
        Platform.runLater(() ->
                panel.showSimulationResult(simulationResult(), Map.of(), List.of(),
                        List.of(referenceData())));
        WaitForAsyncUtils.waitForFxEvents();

        // Switch to Chart tab
        Platform.runLater(() -> {
            TabPane resultTabs = robot.lookup("#dashboardResultTabs").queryAs(TabPane.class);
            TabPane innerTabs = (TabPane) resultTabs.getTabs().getFirst().getContent()
                    .lookup(".tab-pane");
            if (innerTabs != null) {
                innerTabs.getSelectionModel().select(1);
            }
        });
        WaitForAsyncUtils.waitForFxEvents();

        // The chart should have simulation series + reference series
        // Find chart labels containing "(observed)"
        var labels = robot.lookup(".label").queryAllAs(Label.class);
        boolean hasObservedLabel = labels.stream()
                .anyMatch(l -> l.getText() != null && l.getText().contains("(observed)"));
        assertThat(hasObservedLabel).isTrue();
    }

    @Test
    @DisplayName("No Reference Data section when no datasets provided")
    void noReferenceSectionWhenEmpty(FxRobot robot) {
        Platform.runLater(() ->
                panel.showSimulationResult(simulationResult()));
        WaitForAsyncUtils.waitForFxEvents();

        // Switch to Chart tab
        Platform.runLater(() -> {
            TabPane resultTabs = robot.lookup("#dashboardResultTabs").queryAs(TabPane.class);
            TabPane innerTabs = (TabPane) resultTabs.getTabs().getFirst().getContent()
                    .lookup(".tab-pane");
            if (innerTabs != null) {
                innerTabs.getSelectionModel().select(1);
            }
        });
        WaitForAsyncUtils.waitForFxEvents();

        var headers = robot.lookup("#referenceDataHeader").tryQueryAs(Label.class);
        assertThat(headers).isEmpty();
    }

    @Nested
    @DisplayName("Multiple reference datasets")
    @ExtendWith(ApplicationExtension.class)
    class MultipleDatasets {

        private DashboardPanel multiPanel;

        @Start
        void start(Stage stage) {
            multiPanel = new DashboardPanel();
            stage.setScene(new Scene(new StackPane(multiPanel), 800, 600));
            stage.show();
        }

        @Test
        @DisplayName("Multiple reference datasets produce multiple observed series")
        void multipleDatasets(FxRobot robot) {
            ReferenceDataset ds1 = new ReferenceDataset(
                    "Source A", new double[]{0, 1, 2},
                    Map.of("Population", new double[]{100, 108, 114})
            );
            ReferenceDataset ds2 = new ReferenceDataset(
                    "Source B", new double[]{0, 1, 2},
                    Map.of("Revenue", new double[]{50, 54, 57})
            );

            Platform.runLater(() ->
                    multiPanel.showSimulationResult(simulationResult(), Map.of(), List.of(),
                            List.of(ds1, ds2)));
            WaitForAsyncUtils.waitForFxEvents();

            // Switch to Chart tab
            Platform.runLater(() -> {
                TabPane resultTabs = robot.lookup("#dashboardResultTabs").queryAs(TabPane.class);
                TabPane innerTabs = (TabPane) resultTabs.getTabs().getFirst().getContent()
                        .lookup(".tab-pane");
                if (innerTabs != null) {
                    innerTabs.getSelectionModel().select(1);
                }
            });
            WaitForAsyncUtils.waitForFxEvents();

            var labels = robot.lookup(".label").queryAllAs(Label.class);
            long observedCount = labels.stream()
                    .filter(l -> l.getText() != null && l.getText().contains("(observed)"))
                    .count();
            assertThat(observedCount).isGreaterThanOrEqualTo(2);
        }
    }
}

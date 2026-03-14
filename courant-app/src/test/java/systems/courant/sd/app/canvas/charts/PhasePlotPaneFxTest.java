package systems.courant.sd.app.canvas.charts;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
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
import systems.courant.sd.app.canvas.DashboardPanel;
import systems.courant.sd.app.canvas.GhostRun;
import systems.courant.sd.app.canvas.SimulationRunner;

@DisplayName("PhasePlotPane (TestFX)")
@ExtendWith(ApplicationExtension.class)
class PhasePlotPaneFxTest {

    private PhasePlotPane pane;

    private SimulationRunner.SimulationResult twoStockResult() {
        return new SimulationRunner.SimulationResult(
                List.of("Step", "Prey", "Predator"),
                List.of(
                        new double[]{0, 100, 10},
                        new double[]{1, 120, 12},
                        new double[]{2, 110, 18},
                        new double[]{3, 90, 20},
                        new double[]{4, 80, 15}
                ),
                Map.of("Prey", "animals", "Predator", "animals"),
                Set.of("Prey", "Predator")
        );
    }

    private SimulationRunner.SimulationResult threeVarResult() {
        return new SimulationRunner.SimulationResult(
                List.of("Step", "A", "B", "C"),
                List.of(
                        new double[]{0, 1, 2, 3},
                        new double[]{1, 4, 5, 6},
                        new double[]{2, 7, 8, 9}
                ),
                Map.of(),
                Set.of("A")
        );
    }

    @Start
    void start(Stage stage) {
        pane = new PhasePlotPane(twoStockResult(), List.of());
        stage.setScene(new Scene(new StackPane(pane), 800, 600));
        stage.show();
    }

    @Test
    @DisplayName("ComboBoxes are populated with variable names")
    void comboBoxesPopulated(FxRobot robot) {
        ComboBox<String> xCombo = robot.lookup("#phasePlotXCombo").queryComboBox();
        ComboBox<String> yCombo = robot.lookup("#phasePlotYCombo").queryComboBox();

        assertThat(xCombo.getItems()).containsExactly("Prey", "Predator");
        assertThat(yCombo.getItems()).containsExactly("Prey", "Predator");
    }

    @Test
    @DisplayName("First two variables are auto-selected")
    void autoSelection(FxRobot robot) {
        ComboBox<String> xCombo = robot.lookup("#phasePlotXCombo").queryComboBox();
        ComboBox<String> yCombo = robot.lookup("#phasePlotYCombo").queryComboBox();

        assertThat(xCombo.getValue()).isEqualTo("Prey");
        assertThat(yCombo.getValue()).isEqualTo("Predator");
    }

    @Test
    @DisplayName("Chart is rendered with correct number of data points")
    void chartRendered(FxRobot robot) {
        LineChart<Number, Number> chart = robot.lookup("#phasePlotChart").queryAs(LineChart.class);
        assertThat(chart).isNotNull();
        assertThat(chart.getData()).hasSize(1); // current trajectory only
        assertThat(chart.getData().getFirst().getData()).hasSize(5);
    }

    @Test
    @DisplayName("Start marker is a green circle")
    void startMarkerPresent(FxRobot robot) {
        Circle startMarker = robot.lookup("#phasePlotStartMarker").queryAs(Circle.class);
        assertThat(startMarker).isNotNull();
        assertThat(startMarker.getStyle()).contains("#2ca02c");
    }

    @Test
    @DisplayName("End marker is a red square")
    void endMarkerPresent(FxRobot robot) {
        Rectangle endMarker = robot.lookup("#phasePlotEndMarker").queryAs(Rectangle.class);
        assertThat(endMarker).isNotNull();
        assertThat(endMarker.getStyle()).contains("#d62728");
    }

    @Test
    @DisplayName("Axis labels include variable names")
    void axisLabels(FxRobot robot) {
        LineChart<Number, Number> chart = robot.lookup("#phasePlotChart").queryAs(LineChart.class);
        assertThat(chart.getXAxis().getLabel()).isEqualTo("Prey (animals)");
        assertThat(chart.getYAxis().getLabel()).isEqualTo("Predator (animals)");
    }

    @Test
    @DisplayName("Changing axis selection updates the chart")
    void changingAxisUpdatesChart(FxRobot robot) {
        // Swap X and Y by selecting Predator for X
        Platform.runLater(() -> {
            ComboBox<String> xCombo = robot.lookup("#phasePlotXCombo").queryComboBox();
            xCombo.getSelectionModel().select("Predator");
        });
        WaitForAsyncUtils.waitForFxEvents();

        LineChart<Number, Number> chart = robot.lookup("#phasePlotChart").queryAs(LineChart.class);
        assertThat(chart.getXAxis().getLabel()).isEqualTo("Predator (animals)");
        // Y axis should still be Predator (both same now)
        assertThat(chart.getYAxis().getLabel()).isEqualTo("Predator (animals)");
    }

    @Nested
    @DisplayName("Ghost overlays")
    @ExtendWith(ApplicationExtension.class)
    class GhostOverlays {

        private PhasePlotPane ghostPane;

        @Start
        void start(Stage stage) {
            SimulationRunner.SimulationResult ghostResult =
                    new SimulationRunner.SimulationResult(
                            List.of("Step", "Prey", "Predator"),
                            List.of(
                                    new double[]{0, 50, 5},
                                    new double[]{1, 60, 8},
                                    new double[]{2, 55, 12}
                            ),
                            Map.of("Prey", "animals", "Predator", "animals"),
                            Set.of("Prey", "Predator")
                    );
            GhostRun ghost = new GhostRun(ghostResult, "Run 1", 0, Map.of());

            ghostPane = new PhasePlotPane(twoStockResult(), List.of(ghost));
            stage.setScene(new Scene(new StackPane(ghostPane), 800, 600));
            stage.show();
        }

        @Test
        @DisplayName("Ghost trajectory is rendered alongside current")
        void ghostTrajectoryRendered(FxRobot robot) {
            LineChart<Number, Number> chart = robot.lookup("#phasePlotChart").queryAs(LineChart.class);
            // 1 ghost + 1 current = 2 series
            assertThat(chart.getData()).hasSize(2);
            // Ghost series (first) has 3 data points
            assertThat(chart.getData().get(0).getData()).hasSize(3);
            // Current series has 5 data points
            assertThat(chart.getData().get(1).getData()).hasSize(5);
        }
    }

    @Nested
    @DisplayName("Three-variable result")
    @ExtendWith(ApplicationExtension.class)
    class ThreeVariables {

        @Start
        void start(Stage stage) {
            PhasePlotPane threePane = new PhasePlotPane(threeVarResult(), List.of());
            stage.setScene(new Scene(new StackPane(threePane), 800, 600));
            stage.show();
        }

        @Test
        @DisplayName("All three variables available in ComboBoxes")
        void threeVariablesAvailable(FxRobot robot) {
            ComboBox<String> xCombo = robot.lookup("#phasePlotXCombo").queryComboBox();
            ComboBox<String> yCombo = robot.lookup("#phasePlotYCombo").queryComboBox();

            assertThat(xCombo.getItems()).containsExactly("A", "B", "C");
            assertThat(yCombo.getItems()).containsExactly("A", "B", "C");
            assertThat(xCombo.getValue()).isEqualTo("A");
            assertThat(yCombo.getValue()).isEqualTo("B");
        }
    }

    @Nested
    @DisplayName("Dashboard integration")
    @ExtendWith(ApplicationExtension.class)
    class DashboardIntegration {

        private DashboardPanel panel;

        @Start
        void start(Stage stage) {
            panel = new DashboardPanel();
            stage.setScene(new Scene(new StackPane(panel), 800, 600));
            stage.show();
        }

        @Test
        @DisplayName("Phase Plot tab appears for two-stock simulation")
        void phasePlotTabAppears(FxRobot robot) {
            Platform.runLater(() -> panel.showSimulationResult(twoStockResult()));
            WaitForAsyncUtils.waitForFxEvents();

            var tabs = robot.lookup("#dashboardResultTabs")
                    .queryAs(javafx.scene.control.TabPane.class);
            List<String> tabTitles = tabs.getTabs().stream()
                    .map(javafx.scene.control.Tab::getText)
                    .toList();
            assertThat(tabTitles).contains("Phase Plot");
        }

        @Test
        @DisplayName("Phase Plot tab does not appear for single-variable simulation")
        void phasePlotTabAbsentForSingleVar(FxRobot robot) {
            SimulationRunner.SimulationResult singleVar =
                    new SimulationRunner.SimulationResult(
                            List.of("Step", "Population"),
                            List.of(new double[]{0, 100}, new double[]{1, 110})
                    );
            Platform.runLater(() -> panel.showSimulationResult(singleVar));
            WaitForAsyncUtils.waitForFxEvents();

            var tabs = robot.lookup("#dashboardResultTabs")
                    .queryAs(javafx.scene.control.TabPane.class);
            List<String> tabTitles = tabs.getTabs().stream()
                    .map(javafx.scene.control.Tab::getText)
                    .toList();
            assertThat(tabTitles).doesNotContain("Phase Plot");
        }
    }
}

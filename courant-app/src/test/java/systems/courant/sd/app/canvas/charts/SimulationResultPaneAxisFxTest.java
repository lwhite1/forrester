package systems.courant.sd.app.canvas.charts;

import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TabPane;
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
import static org.assertj.core.api.Assertions.within;
import systems.courant.sd.app.canvas.SimulationRunner;

@DisplayName("SimulationResultPane axis behavior (#541, #542)")
@ExtendWith(ApplicationExtension.class)
class SimulationResultPaneAxisFxTest {

    private SimulationResultPane pane;

    @Start
    void start(Stage stage) {
        SimulationRunner.SimulationResult result = new SimulationRunner.SimulationResult(
                List.of("Step", "Population", "BirthRate"),
                List.of(
                        new double[]{0, 100, 10},
                        new double[]{1, 200, 20},
                        new double[]{2, 400, 40},
                        new double[]{3, 300, 30},
                        new double[]{4, 150, 15}
                ),
                Map.of("Population", "People", "BirthRate", "People/Day"),
                Set.of("Population")
        );
        pane = new SimulationResultPane(result, List.of(), List.of(), () -> {});
        stage.setScene(new Scene(pane, 900, 600));
        stage.show();
    }

    @Test
    @DisplayName("x-axis should have explicit bounds matching step range (#541)")
    void xAxisShouldMatchStepRange(FxRobot robot) {
        WaitForAsyncUtils.waitForFxEvents();

        // Switch to chart tab
        TabPane tabPane = robot.lookup(".tab-pane").queryAs(TabPane.class);
        robot.interact(() -> tabPane.getSelectionModel().select(1));
        WaitForAsyncUtils.waitForFxEvents();

        LineChart<?, ?> chart = robot.lookup(".chart").queryAs(LineChart.class);
        NumberAxis xAxis = (NumberAxis) chart.getXAxis();

        assertThat(xAxis.isAutoRanging()).isFalse();
        assertThat(xAxis.getLowerBound()).isCloseTo(0.0, within(0.01));
        assertThat(xAxis.getUpperBound()).isCloseTo(4.0, within(0.01));
    }

    @Test
    @DisplayName("y-axis should use explicit bounds, not auto-ranging (#542)")
    void yAxisShouldNotAutoRange(FxRobot robot) {
        WaitForAsyncUtils.waitForFxEvents();

        TabPane tabPane = robot.lookup(".tab-pane").queryAs(TabPane.class);
        robot.interact(() -> tabPane.getSelectionModel().select(1));
        WaitForAsyncUtils.waitForFxEvents();

        LineChart<?, ?> chart = robot.lookup(".chart").queryAs(LineChart.class);
        NumberAxis yAxis = (NumberAxis) chart.getYAxis();

        assertThat(yAxis.isAutoRanging()).isFalse();
        // Y range should encompass all data: 10..400
        assertThat(yAxis.getLowerBound()).isLessThanOrEqualTo(10.0);
        assertThat(yAxis.getUpperBound()).isGreaterThanOrEqualTo(400.0);
    }

    @Test
    @DisplayName("y-axis should rescale when series unchecked (#542)")
    void yAxisShouldRescaleOnSeriesToggle(FxRobot robot) {
        WaitForAsyncUtils.waitForFxEvents();

        TabPane tabPane = robot.lookup(".tab-pane").queryAs(TabPane.class);
        robot.interact(() -> tabPane.getSelectionModel().select(1));
        WaitForAsyncUtils.waitForFxEvents();

        LineChart<?, ?> chart = robot.lookup(".chart").queryAs(LineChart.class);
        NumberAxis yAxis = (NumberAxis) chart.getYAxis();

        double originalUpper = yAxis.getUpperBound();

        // Uncheck the first checkbox (Population, range 100-400) so only BirthRate (10-40) remains
        CheckBox firstCb = robot.lookup(".check-box").nth(0).queryAs(CheckBox.class);
        robot.interact(() -> firstCb.setSelected(false));
        WaitForAsyncUtils.waitForFxEvents();

        // Y-axis upper bound should have decreased since high-value series was hidden
        assertThat(yAxis.getUpperBound()).isLessThan(originalUpper);
        assertThat(yAxis.getUpperBound()).isLessThan(100.0);
    }

    @Test
    @DisplayName("y-axis should expand when series re-checked (#542)")
    void yAxisShouldExpandOnSeriesRecheck(FxRobot robot) {
        WaitForAsyncUtils.waitForFxEvents();

        TabPane tabPane = robot.lookup(".tab-pane").queryAs(TabPane.class);
        robot.interact(() -> tabPane.getSelectionModel().select(1));
        WaitForAsyncUtils.waitForFxEvents();

        LineChart<?, ?> chart = robot.lookup(".chart").queryAs(LineChart.class);
        NumberAxis yAxis = (NumberAxis) chart.getYAxis();

        // Uncheck then re-check the first checkbox
        CheckBox firstCb = robot.lookup(".check-box").nth(0).queryAs(CheckBox.class);
        robot.interact(() -> firstCb.setSelected(false));
        WaitForAsyncUtils.waitForFxEvents();
        double shrunkUpper = yAxis.getUpperBound();

        robot.interact(() -> firstCb.setSelected(true));
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(yAxis.getUpperBound()).isGreaterThan(shrunkUpper);
        assertThat(yAxis.getUpperBound()).isGreaterThanOrEqualTo(400.0);
    }
}

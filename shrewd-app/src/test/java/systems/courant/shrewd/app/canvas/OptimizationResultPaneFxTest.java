package systems.courant.shrewd.app.canvas;

import systems.courant.shrewd.Simulation;
import systems.courant.shrewd.measure.Quantity;
import systems.courant.shrewd.model.Flow;
import systems.courant.shrewd.model.Model;
import systems.courant.shrewd.model.Stock;
import systems.courant.shrewd.sweep.OptimizationResult;
import systems.courant.shrewd.sweep.RunResult;

import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static systems.courant.shrewd.measure.Units.GALLON_US;
import static systems.courant.shrewd.measure.Units.MINUTE;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OptimizationResultPane (TestFX)")
@ExtendWith(ApplicationExtension.class)
class OptimizationResultPaneFxTest {

    private OptimizationResultPane pane;

    @Start
    void start(Stage stage) {
        OptimizationResult result = buildOptimizationResult();
        pane = new OptimizationResultPane(result);
        stage.setScene(new Scene(pane, 800, 600));
        stage.show();
    }

    @Test
    @DisplayName("Pane contains a LineChart in center")
    void shouldContainLineChart(FxRobot robot) {
        LineChart<?, ?> chart = robot.lookup(".chart").queryAs(LineChart.class);
        assertThat(chart).isNotNull();
        assertThat(chart.animatedProperty().get()).isFalse();
    }

    @Test
    @DisplayName("Chart has series for stocks")
    void chartHasSeriesForStocks(FxRobot robot) {
        LineChart<?, ?> chart = robot.lookup(".chart").queryAs(LineChart.class);
        assertThat(chart.getData()).isNotEmpty();
    }

    @Test
    @DisplayName("Top section contains summary grid with optimization results")
    void topContainsSummaryGrid(FxRobot robot) {
        assertThat(pane.getTop()).isNotNull();

        // Find labels containing "Optimization Results"
        Set<Label> labels = robot.lookup(".label").queryAllAs(Label.class);
        boolean foundHeader = labels.stream()
                .anyMatch(l -> l.getText() != null && l.getText().contains("Optimization Results"));
        assertThat(foundHeader).isTrue();
    }

    @Test
    @DisplayName("Summary grid shows objective value and evaluation count")
    void summaryGridShowsObjectiveAndEvaluations(FxRobot robot) {
        Set<Label> labels = robot.lookup(".label").queryAllAs(Label.class);

        boolean foundObjective = labels.stream()
                .anyMatch(l -> l.getText() != null && l.getText().contains("Objective Value"));
        boolean foundEvaluations = labels.stream()
                .anyMatch(l -> l.getText() != null && l.getText().contains("Evaluations"));

        assertThat(foundObjective).isTrue();
        assertThat(foundEvaluations).isTrue();
    }

    @Test
    @DisplayName("Summary grid shows best parameter values")
    void summaryGridShowsBestParameters(FxRobot robot) {
        Set<Label> labels = robot.lookup(".label").queryAllAs(Label.class);

        boolean foundDrainRate = labels.stream()
                .anyMatch(l -> l.getText() != null && l.getText().contains("drainRate"));
        assertThat(foundDrainRate).isTrue();
    }

    private static OptimizationResult buildOptimizationResult() {
        Map<String, Double> bestParams = new LinkedHashMap<>();
        bestParams.put("drainRate", 7.5);

        RunResult bestRun = buildRunResult(7.5);

        return new OptimizationResult(bestParams, 42.0, bestRun, 100);
    }

    private static RunResult buildRunResult(double drainRate) {
        Model model = new Model("Opt Test");
        Stock tank = new Stock("Tank", 100, GALLON_US);
        Flow outflow = Flow.create("Drain", MINUTE, () -> new Quantity(drainRate, GALLON_US));
        tank.addOutflow(outflow);
        model.addStock(tank);

        RunResult rr = new RunResult(Map.of("drainRate", drainRate));
        Simulation sim = new Simulation(model, MINUTE, MINUTE, 5);
        sim.addEventHandler(rr);
        sim.execute();
        return rr;
    }
}

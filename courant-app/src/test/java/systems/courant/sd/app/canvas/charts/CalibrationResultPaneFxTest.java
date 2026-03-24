package systems.courant.sd.app.canvas.charts;

import systems.courant.sd.Simulation;
import systems.courant.sd.app.canvas.dialogs.CalibrateDialog;
import systems.courant.sd.measure.Quantity;
import systems.courant.sd.model.Flow;
import systems.courant.sd.model.Model;
import systems.courant.sd.model.Stock;
import systems.courant.sd.sweep.OptimizationResult;
import systems.courant.sd.sweep.RunResult;

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
import java.util.List;
import java.util.Map;
import java.util.Set;

import static systems.courant.sd.measure.Units.GALLON_US;
import static systems.courant.sd.measure.Units.MINUTE;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CalibrationResultPane (TestFX)")
@ExtendWith(ApplicationExtension.class)
class CalibrationResultPaneFxTest {

    private CalibrationResultPane pane;

    @Start
    void start(Stage stage) {
        OptimizationResult result = buildOptimizationResult();
        List<CalibrateDialog.FitTarget> fitTargets = List.of(
                new CalibrateDialog.FitTarget("Tank", "Observed_Tank",
                        new double[]{100, 92, 85, 77, 70, 62}));
        pane = new CalibrationResultPane(result, fitTargets, "Day");
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
    @DisplayName("Chart has both simulated and observed series")
    void chartHasSimulatedAndObservedSeries(FxRobot robot) {
        LineChart<?, ?> chart = robot.lookup(".chart").queryAs(LineChart.class);
        assertThat(chart.getData()).hasSize(2);
        boolean foundSimulated = chart.getData().stream()
                .anyMatch(s -> s.getName().contains("simulated"));
        boolean foundObserved = chart.getData().stream()
                .anyMatch(s -> s.getName().contains("observed"));
        assertThat(foundSimulated).isTrue();
        assertThat(foundObserved).isTrue();
    }

    @Test
    @DisplayName("Top section contains summary grid with calibration results")
    void topContainsSummaryGrid(FxRobot robot) {
        assertThat(pane.getTop()).isNotNull();

        Set<Label> labels = robot.lookup(".label").queryAllAs(Label.class);
        boolean foundHeader = labels.stream()
                .anyMatch(l -> l.getText() != null && l.getText().contains("Calibration Results"));
        assertThat(foundHeader).isTrue();
    }

    @Test
    @DisplayName("Summary grid shows SSE and evaluation count")
    void summaryGridShowsSseAndEvaluations(FxRobot robot) {
        Set<Label> labels = robot.lookup(".label").queryAllAs(Label.class);

        boolean foundSse = labels.stream()
                .anyMatch(l -> l.getText() != null && l.getText().contains("SSE"));
        boolean foundEvaluations = labels.stream()
                .anyMatch(l -> l.getText() != null && l.getText().contains("Evaluations"));

        assertThat(foundSse).isTrue();
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
        Model model = new Model("Calib Test");
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

package systems.courant.sd.app.canvas;

import systems.courant.sd.Simulation;
import systems.courant.sd.measure.Quantity;
import systems.courant.sd.model.Flow;
import systems.courant.sd.model.Model;
import systems.courant.sd.model.Stock;
import systems.courant.sd.model.Variable;
import systems.courant.sd.sweep.MonteCarloResult;
import systems.courant.sd.sweep.OptimizationResult;
import systems.courant.sd.sweep.RunResult;
import systems.courant.sd.sweep.SensitivitySummary.ParameterImpact;
import systems.courant.sd.sweep.SweepResult;

import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static systems.courant.sd.measure.Units.DIMENSIONLESS;
import static systems.courant.sd.measure.Units.GALLON_US;
import static systems.courant.sd.measure.Units.MINUTE;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * TestFX test that verifies the full report generation pipeline works when
 * invoked from the JavaFX Application Thread (simulating real UI usage).
 */
@DisplayName("ResultReportGenerator (TestFX)")
@ExtendWith(ApplicationExtension.class)
class ResultReportGeneratorFxTest {

    private final AtomicReference<String> generatedHtml = new AtomicReference<>();

    @Start
    void start(Stage stage) {
        stage.setScene(new Scene(new StackPane(new Label("Report Generator Test")), 400, 300));
        stage.show();
    }

    @Test
    @DisplayName("should generate full report on FX thread with all section types")
    void shouldGenerateFullReportOnFxThread(FxRobot robot) {
        robot.interact(() -> {
            RunResult singleRun = buildSingleRunWithVariable();
            SweepResult sweep = buildSweepResult();
            MonteCarloResult mc = buildMonteCarloResult();
            List<ParameterImpact> impacts = buildImpacts();
            OptimizationResult optimization = buildOptimizationResult();

            String html = ResultReportGenerator.generate("FX Thread Report",
                    singleRun, sweep, mc, impacts, optimization);
            generatedHtml.set(html);
        });

        WaitForAsyncUtils.waitForFxEvents();
        String html = generatedHtml.get();

        assertThat(html).isNotNull();
        assertThat(html).startsWith("<!DOCTYPE html>");
        assertThat(html).contains("FX Thread Report");
        assertThat(html).contains("Simulation Results");
        assertThat(html).contains("Parameter Sweep");
        assertThat(html).contains("Monte Carlo Analysis");
        assertThat(html).contains("Sensitivity Analysis");
        assertThat(html).contains("Optimization Results");
    }

    @Test
    @DisplayName("should produce SVG charts on FX thread")
    void shouldProduceSvgChartsOnFxThread(FxRobot robot) {
        robot.interact(() -> {
            RunResult singleRun = buildSingleRun(10.0);
            String html = ResultReportGenerator.generate("SVG Test",
                    singleRun, null, null, null, null);
            generatedHtml.set(html);
        });

        WaitForAsyncUtils.waitForFxEvents();
        String html = generatedHtml.get();

        // Count SVG elements
        long svgCount = countOccurrences(html, "<svg ");
        assertThat(svgCount).isGreaterThanOrEqualTo(1);
        assertThat(html).contains("<polyline");
        assertThat(html).contains("Stock Time Series");
    }

    @Test
    @DisplayName("should produce fan chart and tornado chart SVG on FX thread")
    void shouldProduceAnalysisChartsOnFxThread(FxRobot robot) {
        robot.interact(() -> {
            MonteCarloResult mc = buildMonteCarloResult();
            List<ParameterImpact> impacts = buildImpacts();

            String html = ResultReportGenerator.generate("Analysis Charts",
                    null, null, mc, impacts, null);
            generatedHtml.set(html);
        });

        WaitForAsyncUtils.waitForFxEvents();
        String html = generatedHtml.get();

        assertThat(html).contains("<polygon");   // fan chart bands
        assertThat(html).contains("Fan Chart");
        assertThat(html).contains("Variance Decomposition");  // tornado chart
    }

    @Test
    @DisplayName("should produce collapsible raw data and percentile tables on FX thread")
    void shouldProduceCollapsibleTablesOnFxThread(FxRobot robot) {
        robot.interact(() -> {
            RunResult run = buildSingleRun(10.0);
            MonteCarloResult mc = buildMonteCarloResult();

            String html = ResultReportGenerator.generate("Tables Test",
                    run, null, mc, null, null);
            generatedHtml.set(html);
        });

        WaitForAsyncUtils.waitForFxEvents();
        String html = generatedHtml.get();

        long detailsCount = countOccurrences(html, "<details");
        assertThat(detailsCount).isGreaterThanOrEqualTo(2);  // raw data + percentile
        assertThat(html).contains("Raw Time Series Data");
        assertThat(html).contains("Percentile Data");
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    private static long countOccurrences(String text, String pattern) {
        long count = 0;
        int idx = 0;
        while ((idx = text.indexOf(pattern, idx)) != -1) {
            count++;
            idx += pattern.length();
        }
        return count;
    }

    private static RunResult buildSingleRun(double drainRate) {
        Model model = new Model("Test");
        Stock tank = new Stock("Tank", 100, GALLON_US);
        Flow outflow = Flow.create("Drain", MINUTE, () -> new Quantity(drainRate, GALLON_US));
        tank.addOutflow(outflow);
        model.addStock(tank);

        RunResult run = new RunResult(drainRate);
        Simulation sim = new Simulation(model, MINUTE, MINUTE, 5);
        sim.addEventHandler(run);
        sim.execute();
        return run;
    }

    private static RunResult buildSingleRunWithVariable() {
        Model model = new Model("VarTest");
        Stock tank = new Stock("Tank", 100, GALLON_US);
        Flow outflow = Flow.create("Drain", MINUTE, () -> new Quantity(10, GALLON_US));
        tank.addOutflow(outflow);
        model.addStock(tank);

        Variable indicator = new Variable("level_indicator", DIMENSIONLESS,
                () -> tank.getValue() > 50 ? 1.0 : 0.0);
        model.addVariable(indicator);

        RunResult run = new RunResult(10.0);
        Simulation sim = new Simulation(model, MINUTE, MINUTE, 5);
        sim.addEventHandler(run);
        sim.execute();
        return run;
    }

    private static SweepResult buildSweepResult() {
        List<RunResult> runs = new ArrayList<>();
        for (double rate : new double[]{5.0, 10.0, 15.0}) {
            runs.add(buildSingleRun(rate));
        }
        return new SweepResult("drainRate", runs);
    }

    private static MonteCarloResult buildMonteCarloResult() {
        List<RunResult> runs = new ArrayList<>();
        for (double rate : new double[]{5.0, 7.0, 10.0, 12.0, 15.0}) {
            runs.add(buildSingleRun(rate));
        }
        return new MonteCarloResult(runs);
    }

    private static List<ParameterImpact> buildImpacts() {
        return List.of(
                new ParameterImpact("contactRate", "Infectious", 50, 500, 200, 0.70),
                new ParameterImpact("recoveryRate", "Infectious", 100, 300, 200, 0.30)
        );
    }

    private static OptimizationResult buildOptimizationResult() {
        RunResult bestRun = buildSingleRun(7.5);
        Map<String, Double> bestParams = new LinkedHashMap<>();
        bestParams.put("drainRate", 7.5);
        return new OptimizationResult(bestParams, 42.0, bestRun, 100);
    }
}

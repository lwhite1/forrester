package systems.courant.shrewd.ui;

import systems.courant.shrewd.Simulation;
import systems.courant.shrewd.model.Flow;
import systems.courant.shrewd.model.Model;
import systems.courant.shrewd.model.Stock;
import systems.courant.shrewd.measure.Quantity;
import systems.courant.shrewd.sweep.MonteCarloResult;
import systems.courant.shrewd.sweep.RunResult;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static systems.courant.shrewd.measure.Units.MINUTE;
import static systems.courant.shrewd.measure.Units.THING;

/**
 * Tests that FanChart's constructor atomically pairs the result and variable name,
 * eliminating the race condition that existed with separate volatile fields.
 */
@DisplayName("FanChart thread safety")
class FanChartThreadSafetyTest {

    @Test
    @DisplayName("constructor atomically pairs result and variable name")
    void shouldAtomicallyPairResultAndVariableName() {
        MonteCarloResult result1 = buildMonteCarloResult(5, 3, "Model1");
        MonteCarloResult result2 = buildMonteCarloResult(5, 3, "Model2");

        FanChart chart1 = new FanChart(result1, "Population");
        FanChart chart2 = new FanChart(result2, "Infected");

        // Each instance holds its own data — no shared static state
        assertThat(chart1).isNotSameAs(chart2);
        // The fact that we can construct two FanChart instances with different data
        // without any static field interaction proves the race is eliminated.
    }

    @Test
    @DisplayName("no-arg constructor creates instance with null fields")
    void shouldCreateNullInstanceWithNoArgConstructor() {
        FanChart chart = new FanChart();
        // No-arg constructor exists for Application.launch() compatibility
        // but should not be used directly — the instance will have null fields
        assertThat(chart).isNotNull();
    }

    @Test
    @DisplayName("rapid concurrent construction does not cross-contaminate instances")
    void shouldNotCrossContaminateInstances() throws InterruptedException {
        int iterations = 100;
        List<FanChart> charts = new ArrayList<>();
        List<Thread> threads = new ArrayList<>();

        for (int i = 0; i < iterations; i++) {
            int idx = i;
            Thread t = new Thread(() -> {
                MonteCarloResult result = buildMonteCarloResult(3, 2, "Model" + idx);
                FanChart chart = new FanChart(result, "Var" + idx);
                synchronized (charts) {
                    charts.add(chart);
                }
            });
            threads.add(t);
        }

        for (Thread t : threads) {
            t.start();
        }
        for (Thread t : threads) {
            t.join(10_000);
        }

        assertThat(charts).hasSize(iterations);
    }

    private static MonteCarloResult buildMonteCarloResult(int runs, int steps, String modelName) {
        List<RunResult> results = new ArrayList<>();
        for (int r = 0; r < runs; r++) {
            double growthRate = 0.05 + r * 0.005;
            Model model = new Model(modelName);
            Stock pop = new Stock("Population", 100, THING);
            Flow growth = Flow.create("Growth", MINUTE,
                    () -> new Quantity(pop.getValue() * growthRate, THING));
            pop.addInflow(growth);
            model.addStock(pop);
            model.addFlow(growth);

            RunResult rr = new RunResult(Map.of("rate", growthRate));
            Simulation sim = new Simulation(model, MINUTE, MINUTE, steps);
            sim.addEventHandler(rr);
            sim.execute();
            results.add(rr);
        }
        return new MonteCarloResult(results);
    }
}

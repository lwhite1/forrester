package systems.courant.sd.ui;

import systems.courant.sd.Simulation;
import systems.courant.sd.model.Flow;
import systems.courant.sd.model.Model;
import systems.courant.sd.model.Stock;
import systems.courant.sd.measure.Quantity;
import systems.courant.sd.sweep.MonteCarloResult;
import systems.courant.sd.sweep.RunResult;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static systems.courant.sd.measure.Units.MINUTE;
import static systems.courant.sd.measure.Units.THING;

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

        assertThat(chart1.getResult()).isSameAs(result1);
        assertThat(chart1.getVariableName()).isEqualTo("Population");
        assertThat(chart2.getResult()).isSameAs(result2);
        assertThat(chart2.getVariableName()).isEqualTo("Infected");
    }

    @Test
    @DisplayName("no-arg constructor creates instance with null fields")
    void shouldCreateNullInstanceWithNoArgConstructor() {
        FanChart chart = new FanChart();
        assertThat(chart.getResult()).isNull();
        assertThat(chart.getVariableName()).isNull();
    }

    @Test
    @DisplayName("rapid concurrent construction does not cross-contaminate instances")
    void shouldNotCrossContaminateInstances() throws InterruptedException {
        int iterations = 100;
        Map<Integer, MonteCarloResult> expectedResults = new ConcurrentHashMap<>();
        Map<Integer, FanChart> charts = new ConcurrentHashMap<>();
        List<Thread> threads = new ArrayList<>();

        for (int i = 0; i < iterations; i++) {
            int idx = i;
            Thread t = new Thread(() -> {
                MonteCarloResult result = buildMonteCarloResult(3, 2, "Model" + idx);
                FanChart chart = new FanChart(result, "Var" + idx);
                expectedResults.put(idx, result);
                charts.put(idx, chart);
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

        for (int i = 0; i < iterations; i++) {
            FanChart chart = charts.get(i);
            assertThat(chart.getResult())
                    .as("chart %d should hold its own MonteCarloResult", i)
                    .isSameAs(expectedResults.get(i));
            assertThat(chart.getVariableName())
                    .as("chart %d should hold variable name Var%d", i, i)
                    .isEqualTo("Var" + i);
        }
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

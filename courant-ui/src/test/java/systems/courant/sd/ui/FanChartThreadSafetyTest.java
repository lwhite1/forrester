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

import java.lang.reflect.Field;
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
    void shouldAtomicallyPairResultAndVariableName() throws Exception {
        MonteCarloResult result1 = buildMonteCarloResult(5, 3, "Model1");
        MonteCarloResult result2 = buildMonteCarloResult(5, 3, "Model2");

        FanChart chart1 = new FanChart(result1, "Population");
        FanChart chart2 = new FanChart(result2, "Infected");

        // Verify each instance holds the correct result and variable name
        assertThat(getField(chart1, "result")).isSameAs(result1);
        assertThat(getField(chart1, "variableName")).isEqualTo("Population");
        assertThat(getField(chart2, "result")).isSameAs(result2);
        assertThat(getField(chart2, "variableName")).isEqualTo("Infected");
    }

    @Test
    @DisplayName("no-arg constructor creates instance with null fields")
    void shouldCreateNullInstanceWithNoArgConstructor() throws Exception {
        FanChart chart = new FanChart();
        // No-arg constructor exists for Application.launch() compatibility
        // but should not be used directly — the instance will have null fields
        assertThat(getField(chart, "result")).isNull();
        assertThat(getField(chart, "variableName")).isNull();
    }

    @Test
    @DisplayName("rapid concurrent construction does not cross-contaminate instances")
    void shouldNotCrossContaminateInstances() throws Exception {
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

        // Verify every instance holds the correct result and variable name
        for (int i = 0; i < iterations; i++) {
            FanChart chart = charts.get(i);
            assertThat(getField(chart, "result"))
                    .as("chart %d should hold its own MonteCarloResult", i)
                    .isSameAs(expectedResults.get(i));
            assertThat(getField(chart, "variableName"))
                    .as("chart %d should hold variable name Var%d", i, i)
                    .isEqualTo("Var" + i);
        }
    }

    private static Object getField(FanChart chart, String fieldName) throws Exception {
        Field field = FanChart.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(chart);
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

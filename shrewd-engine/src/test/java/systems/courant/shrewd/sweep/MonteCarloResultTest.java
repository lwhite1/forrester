package systems.courant.shrewd.sweep;

import systems.courant.shrewd.Simulation;
import systems.courant.shrewd.model.Flow;
import systems.courant.shrewd.model.Model;
import systems.courant.shrewd.model.Stock;
import systems.courant.shrewd.measure.Quantity;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static systems.courant.shrewd.measure.Units.MINUTE;
import static systems.courant.shrewd.measure.Units.THING;

@DisplayName("MonteCarloResult")
class MonteCarloResultTest {

    private static MonteCarloResult result;

    @BeforeAll
    static void setUp() {
        result = buildMonteCarloResult(50, 10);
    }

    @Nested
    @DisplayName("Batch getPercentileSeries")
    class BatchPercentile {

        @Test
        void shouldReturnAllRequestedPercentiles() {
            Map<Double, double[]> pctMap = result.getPercentileSeries("Population",
                    2.5, 25.0, 50.0, 75.0, 97.5);

            assertThat(pctMap).hasSize(5);
            assertThat(pctMap).containsKeys(2.5, 25.0, 50.0, 75.0, 97.5);
        }

        @Test
        void shouldReturnCorrectLengthSeries() {
            Map<Double, double[]> pctMap = result.getPercentileSeries("Population",
                    25.0, 50.0);

            assertThat(pctMap.get(50.0)).hasSize(result.getStepCount());
            assertThat(pctMap.get(25.0)).hasSize(result.getStepCount());
        }

        @Test
        void shouldMatchSingleCallResults() {
            double[] singleP50 = result.getPercentileSeries("Population", 50);
            double[] singleP25 = result.getPercentileSeries("Population", 25);
            double[] singleP97_5 = result.getPercentileSeries("Population", 97.5);

            Map<Double, double[]> batchMap = result.getPercentileSeries("Population",
                    25.0, 50.0, 97.5);

            for (int i = 0; i < result.getStepCount(); i++) {
                assertThat(batchMap.get(50.0)[i]).isCloseTo(singleP50[i], within(1e-10));
                assertThat(batchMap.get(25.0)[i]).isCloseTo(singleP25[i], within(1e-10));
                assertThat(batchMap.get(97.5)[i]).isCloseTo(singleP97_5[i], within(1e-10));
            }
        }

        @Test
        void shouldMaintainPercentileOrdering() {
            Map<Double, double[]> pctMap = result.getPercentileSeries("Population",
                    2.5, 50.0, 97.5);

            for (int i = 0; i < result.getStepCount(); i++) {
                assertThat(pctMap.get(2.5)[i]).isLessThanOrEqualTo(pctMap.get(50.0)[i]);
                assertThat(pctMap.get(50.0)[i]).isLessThanOrEqualTo(pctMap.get(97.5)[i]);
            }
        }

        @Test
        void shouldHandleTwoPercentiles() {
            Map<Double, double[]> pctMap = result.getPercentileSeries("Population",
                    25.0, 75.0);

            assertThat(pctMap).hasSize(2);
            assertThat(pctMap.get(25.0)).isNotNull();
            assertThat(pctMap.get(75.0)).isNotNull();
        }

        @Test
        void shouldWorkForAllSevenFanChartPercentiles() {
            Map<Double, double[]> pctMap = result.getPercentileSeries("Population",
                    2.5, 12.5, 25.0, 50.0, 75.0, 87.5, 97.5);

            assertThat(pctMap).hasSize(7);
            for (double[] series : pctMap.values()) {
                assertThat(series).hasSize(result.getStepCount());
            }
        }
    }

    @Nested
    @DisplayName("Single getPercentileSeries")
    class SinglePercentile {

        @Test
        void shouldReturnMedianSeries() {
            double[] median = result.getPercentileSeries("Population", 50);

            assertThat(median).hasSize(result.getStepCount());
            // First step should be initial value (100) for all runs
            assertThat(median[0]).isCloseTo(100.0, within(0.01));
        }

        @Test
        void shouldReturnMonotonicallyIncreasingMedian() {
            double[] median = result.getPercentileSeries("Population", 50);

            for (int i = 1; i < median.length; i++) {
                assertThat(median[i]).isGreaterThanOrEqualTo(median[i - 1]);
            }
        }
    }

    private static MonteCarloResult buildMonteCarloResult(int runs, int steps) {
        List<RunResult> results = new ArrayList<>();
        for (int r = 0; r < runs; r++) {
            double growthRate = 0.05 + r * 0.005;
            Model model = new Model("TestModel");
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

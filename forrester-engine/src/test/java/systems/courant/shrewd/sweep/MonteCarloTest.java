package systems.courant.forrester.sweep;

import systems.courant.forrester.measure.Quantity;
import systems.courant.forrester.measure.units.time.Times;
import systems.courant.forrester.model.Flow;
import systems.courant.forrester.model.Model;
import systems.courant.forrester.model.Stock;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.UniformRealDistribution;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;

import static systems.courant.forrester.measure.Units.DAY;
import static systems.courant.forrester.measure.Units.THING;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MonteCarloTest {

    @Test
    void shouldRunCorrectNumberOfIterations() {
        MonteCarloResult result = MonteCarlo.builder()
                .parameter("Growth Rate", new NormalDistribution(0.05, 0.01))
                .modelFactory(params -> buildGrowthModel(params.get("Growth Rate")))
                .iterations(50)
                .seed(42L)
                .timeStep(DAY)
                .duration(Times.weeks(2))
                .build()
                .execute();

        assertEquals(50, result.getRunCount());
    }

    @Test
    void shouldProducePercentileEnvelope() {
        MonteCarloResult result = MonteCarlo.builder()
                .parameter("Growth Rate", new NormalDistribution(0.05, 0.01))
                .modelFactory(params -> buildGrowthModel(params.get("Growth Rate")))
                .iterations(100)
                .seed(42L)
                .timeStep(DAY)
                .duration(Times.weeks(4))
                .build()
                .execute();

        double[] p2_5 = result.getPercentileSeries("Population", 2.5);
        double[] p50 = result.getPercentileSeries("Population", 50);
        double[] p97_5 = result.getPercentileSeries("Population", 97.5);

        // At each step (after the first), the percentile ordering should hold
        for (int step = 1; step < result.getStepCount(); step++) {
            assertTrue(p2_5[step] <= p50[step],
                    "P2.5 should be <= P50 at step " + step);
            assertTrue(p50[step] <= p97_5[step],
                    "P50 should be <= P97.5 at step " + step);
        }
    }

    @Test
    void shouldReproduceWithSameSeed() {
        MonteCarlo.Builder baseBuilder = MonteCarlo.builder()
                .parameter("Growth Rate", new NormalDistribution(0.05, 0.01))
                .modelFactory(params -> buildGrowthModel(params.get("Growth Rate")))
                .iterations(30)
                .seed(99L)
                .timeStep(DAY)
                .duration(Times.weeks(2));

        MonteCarloResult result1 = baseBuilder.build().execute();
        MonteCarloResult result2 = baseBuilder.build().execute();

        double[] median1 = result1.getPercentileSeries("Population", 50);
        double[] median2 = result2.getPercentileSeries("Population", 50);

        assertArrayEquals(median1, median2, 0.0001,
                "Same seed should produce identical median series");
    }

    @Test
    void shouldSupportLatinHypercubeSampling() {
        MonteCarloResult randomResult = MonteCarlo.builder()
                .parameter("Growth Rate", new NormalDistribution(0.05, 0.01))
                .modelFactory(params -> buildGrowthModel(params.get("Growth Rate")))
                .iterations(40)
                .sampling(SamplingMethod.RANDOM)
                .seed(42L)
                .timeStep(DAY)
                .duration(Times.weeks(2))
                .build()
                .execute();

        MonteCarloResult lhsResult = MonteCarlo.builder()
                .parameter("Growth Rate", new NormalDistribution(0.05, 0.01))
                .modelFactory(params -> buildGrowthModel(params.get("Growth Rate")))
                .iterations(40)
                .sampling(SamplingMethod.LATIN_HYPERCUBE)
                .seed(42L)
                .timeStep(DAY)
                .duration(Times.weeks(2))
                .build()
                .execute();

        assertEquals(randomResult.getRunCount(), lhsResult.getRunCount());
        assertEquals(randomResult.getStepCount(), lhsResult.getStepCount());
    }

    @Test
    void shouldComputeMeanSeries() {
        MonteCarloResult result = MonteCarlo.builder()
                .parameter("Growth Rate", new NormalDistribution(0.05, 0.01))
                .modelFactory(params -> buildGrowthModel(params.get("Growth Rate")))
                .iterations(50)
                .seed(42L)
                .timeStep(DAY)
                .duration(Times.weeks(2))
                .build()
                .execute();

        double[] mean = result.getMeanSeries("Population");
        double[] p0 = result.getPercentileSeries("Population", 0.1);
        double[] p100 = result.getPercentileSeries("Population", 99.9);

        for (int step = 0; step < result.getStepCount(); step++) {
            assertTrue(mean[step] >= p0[step],
                    "Mean should be >= near-minimum at step " + step);
            assertTrue(mean[step] <= p100[step],
                    "Mean should be <= near-maximum at step " + step);
        }
    }

    @Test
    void shouldRejectMissingModelFactory() {
        assertThrows(IllegalStateException.class, () ->
                MonteCarlo.builder()
                        .parameter("Growth Rate", new NormalDistribution(0.05, 0.01))
                        .timeStep(DAY)
                        .duration(Times.weeks(1))
                        .build());
    }

    @Test
    void shouldWritePercentileCsv(@TempDir Path tempDir) {
        MonteCarloResult result = MonteCarlo.builder()
                .parameter("Growth Rate", new NormalDistribution(0.05, 0.01))
                .modelFactory(params -> buildGrowthModel(params.get("Growth Rate")))
                .iterations(30)
                .seed(42L)
                .timeStep(DAY)
                .duration(Times.weeks(2))
                .build()
                .execute();

        String filePath = tempDir.resolve("percentiles.csv").toString();
        result.writePercentileCsv(filePath, "Population", 2.5, 25, 50, 75, 97.5);

        File file = new File(filePath);
        assertTrue(file.exists());
        assertTrue(file.length() > 0);
    }

    @Test
    void shouldSupportMultipleParameters() {
        MonteCarloResult result = MonteCarlo.builder()
                .parameter("Growth Rate", new NormalDistribution(0.05, 0.01))
                .parameter("Capacity", new UniformRealDistribution(500, 1500))
                .modelFactory(params -> buildGrowthModel(params.get("Growth Rate")))
                .iterations(20)
                .seed(42L)
                .timeStep(DAY)
                .duration(Times.weeks(1))
                .build()
                .execute();

        assertEquals(20, result.getRunCount());
    }

    /**
     * Builds a simple exponential growth model: one stock (Population) with an inflow
     * proportional to the growth rate. Keeps tests fast.
     */
    private Model buildGrowthModel(double growthRate) {
        Model model = new Model("Growth");

        Stock population = new Stock("Population", 100, THING);

        Flow growth = Flow.create("Growth", DAY, () -> {
            double currentPop = population.getQuantity().getValue();
            return new Quantity(currentPop * growthRate, THING);
        });

        population.addInflow(growth);
        model.addStock(population);

        return model;
    }
}

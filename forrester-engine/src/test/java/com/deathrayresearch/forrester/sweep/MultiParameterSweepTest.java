package com.deathrayresearch.forrester.sweep;

import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.units.time.Times;
import com.deathrayresearch.forrester.model.Flow;
import com.deathrayresearch.forrester.model.Model;
import com.deathrayresearch.forrester.model.Stock;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;

import static com.deathrayresearch.forrester.measure.Units.DAY;
import static com.deathrayresearch.forrester.measure.Units.THING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MultiParameterSweepTest {

    @Test
    void shouldRunCartesianProductOfParameters() {
        MultiSweepResult result = MultiParameterSweep.builder()
                .parameter("Rate", new double[]{1.0, 2.0, 3.0})
                .parameter("Scale", new double[]{10.0, 20.0})
                .modelFactory(params -> buildGrowthModel(params.get("Rate")))
                .timeStep(DAY)
                .duration(Times.weeks(1))
                .build()
                .execute();

        // 3 x 2 = 6 combinations
        assertEquals(6, result.getRunCount());
    }

    @Test
    void shouldPreserveParameterNames() {
        MultiSweepResult result = MultiParameterSweep.builder()
                .parameter("Alpha", new double[]{1.0})
                .parameter("Beta", new double[]{2.0})
                .modelFactory(params -> buildGrowthModel(params.get("Alpha")))
                .timeStep(DAY)
                .duration(Times.weeks(1))
                .build()
                .execute();

        assertEquals(2, result.getParameterNames().size());
        assertEquals("Alpha", result.getParameterNames().get(0));
        assertEquals("Beta", result.getParameterNames().get(1));
    }

    @Test
    void shouldStoreParameterMapInRunResult() {
        MultiSweepResult result = MultiParameterSweep.builder()
                .parameter("Rate", new double[]{5.0})
                .parameter("Scale", new double[]{100.0})
                .modelFactory(params -> buildGrowthModel(params.get("Rate")))
                .timeStep(DAY)
                .duration(Times.weeks(1))
                .build()
                .execute();

        assertEquals(1, result.getRunCount());
        Map<String, Double> paramMap = result.getResult(0).getParameterMap();
        assertEquals(5.0, paramMap.get("Rate"), 0.001);
        assertEquals(100.0, paramMap.get("Scale"), 0.001);
    }

    @Test
    void shouldCaptureStockNames() {
        MultiSweepResult result = MultiParameterSweep.builder()
                .parameter("Rate", new double[]{0.05})
                .modelFactory(params -> buildGrowthModel(params.get("Rate")))
                .timeStep(DAY)
                .duration(Times.weeks(1))
                .build()
                .execute();

        assertEquals(1, result.getStockNames().size());
        assertEquals("Population", result.getStockNames().get(0));
    }

    @Test
    void shouldProduceDifferentResultsForDifferentParameters() {
        MultiSweepResult result = MultiParameterSweep.builder()
                .parameter("Rate", new double[]{0.01, 0.10})
                .modelFactory(params -> buildGrowthModel(params.get("Rate")))
                .timeStep(DAY)
                .duration(Times.weeks(2))
                .build()
                .execute();

        double lowRateFinal = result.getResult(0).getFinalStockValue("Population");
        double highRateFinal = result.getResult(1).getFinalStockValue("Population");

        assertTrue(highRateFinal > lowRateFinal,
                "Higher growth rate should produce larger final population");
    }

    @Test
    void shouldWriteTimeSeriesCsv(@TempDir Path tempDir) {
        MultiSweepResult result = MultiParameterSweep.builder()
                .parameter("Rate", new double[]{0.05, 0.10})
                .parameter("Scale", new double[]{1.0})
                .modelFactory(params -> buildGrowthModel(params.get("Rate")))
                .timeStep(DAY)
                .duration(Times.weeks(1))
                .build()
                .execute();

        String filePath = tempDir.resolve("timeseries.csv").toString();
        result.writeTimeSeriesCsv(filePath);

        File file = new File(filePath);
        assertTrue(file.exists());
        assertTrue(file.length() > 0);
    }

    @Test
    void shouldWriteSummaryCsv(@TempDir Path tempDir) {
        MultiSweepResult result = MultiParameterSweep.builder()
                .parameter("Rate", new double[]{0.05, 0.10})
                .parameter("Scale", new double[]{1.0})
                .modelFactory(params -> buildGrowthModel(params.get("Rate")))
                .timeStep(DAY)
                .duration(Times.weeks(1))
                .build()
                .execute();

        String filePath = tempDir.resolve("summary.csv").toString();
        result.writeSummaryCsv(filePath);

        File file = new File(filePath);
        assertTrue(file.exists());
        assertTrue(file.length() > 0);
    }

    @Test
    void shouldRejectMissingModelFactory() {
        assertThrows(IllegalStateException.class, () ->
                MultiParameterSweep.builder()
                        .parameter("Rate", new double[]{1.0})
                        .timeStep(DAY)
                        .duration(Times.weeks(1))
                        .build());
    }

    @Test
    void shouldRejectEmptyParameters() {
        assertThrows(IllegalStateException.class, () ->
                MultiParameterSweep.builder()
                        .modelFactory(params -> buildGrowthModel(0.05))
                        .timeStep(DAY)
                        .duration(Times.weeks(1))
                        .build());
    }

    @Test
    void shouldRejectEmptyParameterValues() {
        assertThrows(IllegalStateException.class, () ->
                MultiParameterSweep.builder()
                        .parameter("Rate", new double[]{})
                        .modelFactory(params -> buildGrowthModel(0.05))
                        .timeStep(DAY)
                        .duration(Times.weeks(1))
                        .build());
    }

    @Test
    void runResultParameterMapShouldBeEmptyForSingleParamConstructor() {
        RunResult result = new RunResult(42.5);
        assertTrue(result.getParameterMap().isEmpty());
        assertEquals(42.5, result.getParameterValue(), 0.001);
    }

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

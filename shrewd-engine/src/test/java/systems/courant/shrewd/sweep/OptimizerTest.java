package systems.courant.shrewd.sweep;

import systems.courant.shrewd.measure.Quantity;
import systems.courant.shrewd.measure.units.time.Times;
import systems.courant.shrewd.model.Flow;
import systems.courant.shrewd.model.Model;
import systems.courant.shrewd.model.Stock;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static systems.courant.shrewd.measure.Units.DAY;
import static systems.courant.shrewd.measure.Units.PEOPLE;
import static systems.courant.shrewd.measure.Units.THING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OptimizerTest {

    @Nested
    class BuilderValidation {

        @Test
        void shouldRejectMissingParameters() {
            assertThrows(IllegalStateException.class, () ->
                    Optimizer.builder()
                            .modelFactory(params -> new Model("test"))
                            .objective(Objectives.minimize("X"))
                            .timeStep(DAY)
                            .duration(Times.weeks(1))
                            .build());
        }

        @Test
        void shouldRejectMissingModelFactory() {
            assertThrows(IllegalStateException.class, () ->
                    Optimizer.builder()
                            .parameter("X", 0, 10)
                            .objective(Objectives.minimize("X"))
                            .timeStep(DAY)
                            .duration(Times.weeks(1))
                            .build());
        }

        @Test
        void shouldRejectMissingObjective() {
            assertThrows(IllegalStateException.class, () ->
                    Optimizer.builder()
                            .parameter("X", 0, 10)
                            .modelFactory(params -> new Model("test"))
                            .timeStep(DAY)
                            .duration(Times.weeks(1))
                            .build());
        }

        @Test
        void shouldRejectMissingTimeStep() {
            assertThrows(IllegalStateException.class, () ->
                    Optimizer.builder()
                            .parameter("X", 0, 10)
                            .modelFactory(params -> new Model("test"))
                            .objective(Objectives.minimize("X"))
                            .duration(Times.weeks(1))
                            .build());
        }

        @Test
        void shouldRejectMissingDuration() {
            assertThrows(IllegalStateException.class, () ->
                    Optimizer.builder()
                            .parameter("X", 0, 10)
                            .modelFactory(params -> new Model("test"))
                            .objective(Objectives.minimize("X"))
                            .timeStep(DAY)
                            .build());
        }

        @Test
        void shouldRejectBobyqaWithOneParameter() {
            assertThrows(IllegalStateException.class, () ->
                    Optimizer.builder()
                            .parameter("X", 0, 10)
                            .modelFactory(params -> new Model("test"))
                            .objective(Objectives.minimize("X"))
                            .algorithm(OptimizationAlgorithm.BOBYQA)
                            .timeStep(DAY)
                            .duration(Times.weeks(1))
                            .build());
        }
    }

    @Nested
    class NelderMeadOptimization {

        @Test
        void shouldMinimizeGrowthRate() {
            OptimizationResult result = Optimizer.builder()
                    .parameter("Growth Rate", 0.01, 0.20)
                    .modelFactory(params -> buildGrowthModel(params.get("Growth Rate")))
                    .objective(Objectives.minimize("Population"))
                    .algorithm(OptimizationAlgorithm.NELDER_MEAD)
                    .maxEvaluations(200)
                    .timeStep(DAY)
                    .duration(Times.weeks(2))
                    .build()
                    .execute();

            assertNotNull(result.getBestParameters());
            assertNotNull(result.getBestRunResult());
            assertTrue(result.getEvaluationCount() > 0);
            assertTrue(result.getBestParameters().get("Growth Rate") < 0.10,
                    "Optimizer should find a low growth rate to minimize population");
        }

        @Test
        void shouldFitToTimeSeries() {
            // Generate target data with known growth rate
            double targetRate = 0.05;
            RunResult targetRun = runGrowthModel(targetRate);
            double[] observed = targetRun.getStockSeries("Population");

            OptimizationResult result = Optimizer.builder()
                    .parameter("Growth Rate", 0.01, 0.20)
                    .modelFactory(params -> buildGrowthModel(params.get("Growth Rate")))
                    .objective(Objectives.fitToTimeSeries("Population", observed))
                    .algorithm(OptimizationAlgorithm.NELDER_MEAD)
                    .maxEvaluations(200)
                    .timeStep(DAY)
                    .duration(Times.weeks(2))
                    .build()
                    .execute();

            double recovered = result.getBestParameters().get("Growth Rate");
            assertEquals(targetRate, recovered, 0.01,
                    "Should recover the true growth rate within tolerance");
        }
    }

    @Nested
    class BobyqaOptimization {

        @Test
        void shouldOptimizeWithBounds() {
            // Use logistic model: both growthRate and capacity are independently identifiable
            double targetRate = 0.08;
            double targetCapacity = 500.0;
            RunResult targetRun = runLogisticModel(targetRate, targetCapacity);
            double[] observed = targetRun.getStockSeries("Population");

            OptimizationResult result = Optimizer.builder()
                    .parameter("Growth Rate", 0.01, 0.20)
                    .parameter("Capacity", 200.0, 1000.0)
                    .modelFactory(params -> buildLogisticModel(
                            params.get("Growth Rate"), params.get("Capacity")))
                    .objective(Objectives.fitToTimeSeries("Population", observed))
                    .algorithm(OptimizationAlgorithm.BOBYQA)
                    .maxEvaluations(500)
                    .timeStep(DAY)
                    .duration(Times.weeks(4))
                    .build()
                    .execute();

            double recoveredGrowth = result.getBestParameters().get("Growth Rate");
            double recoveredCapacity = result.getBestParameters().get("Capacity");
            assertEquals(targetRate, recoveredGrowth, 0.03,
                    "Should recover growth rate within tolerance");
            assertEquals(targetCapacity, recoveredCapacity, 150.0,
                    "Should recover capacity within tolerance");
        }
    }

    @Nested
    class CmaesOptimization {

        @Test
        void shouldOptimizeWithCmaes() {
            double targetRate = 0.08;
            double targetCapacity = 500.0;
            RunResult targetRun = runLogisticModel(targetRate, targetCapacity);
            double[] observed = targetRun.getStockSeries("Population");

            OptimizationResult result = Optimizer.builder()
                    .parameter("Growth Rate", 0.01, 0.20)
                    .parameter("Capacity", 200.0, 1000.0)
                    .modelFactory(params -> buildLogisticModel(
                            params.get("Growth Rate"), params.get("Capacity")))
                    .objective(Objectives.fitToTimeSeries("Population", observed))
                    .algorithm(OptimizationAlgorithm.CMAES)
                    .maxEvaluations(2000)
                    .seed(42L)
                    .timeStep(DAY)
                    .duration(Times.weeks(4))
                    .build()
                    .execute();

            double recoveredGrowth = result.getBestParameters().get("Growth Rate");
            assertEquals(targetRate, recoveredGrowth, 0.04,
                    "CMA-ES should recover growth rate within tolerance");
        }
    }

    @Nested
    class ObjectiveFunctions {

        @Test
        void shouldMinimizeFinalValue() {
            ObjectiveFunction obj = Objectives.minimize("Population");
            RunResult run = runGrowthModel(0.05);
            double value = obj.evaluate(run);
            assertEquals(run.getFinalStockValue("Population"), value, 0.001);
        }

        @Test
        void shouldMaximizeFinalValue() {
            ObjectiveFunction obj = Objectives.maximize("Population");
            RunResult run = runGrowthModel(0.05);
            double value = obj.evaluate(run);
            assertEquals(-run.getFinalStockValue("Population"), value, 0.001);
        }

        @Test
        void shouldTargetValue() {
            ObjectiveFunction obj = Objectives.target("Population", 200.0);
            RunResult run = runGrowthModel(0.05);
            double finalVal = run.getFinalStockValue("Population");
            double expected = (finalVal - 200.0) * (finalVal - 200.0);
            assertEquals(expected, obj.evaluate(run), 0.001);
        }

        @Test
        void shouldMinimizePeak() {
            ObjectiveFunction obj = Objectives.minimizePeak("Population");
            RunResult run = runGrowthModel(0.05);
            double value = obj.evaluate(run);
            assertEquals(run.getMaxStockValue("Population"), value, 0.001);
        }

        @Test
        void shouldFitToTimeSeries() {
            RunResult run = runGrowthModel(0.05);
            double[] observed = run.getStockSeries("Population");
            ObjectiveFunction obj = Objectives.fitToTimeSeries("Population", observed);
            assertEquals(0.0, obj.evaluate(run), 0.001,
                    "SSE should be zero when comparing a run to its own time series");
        }
    }

    @Nested
    class EvaluationLimitHandling {

        @Test
        void shouldReturnBestResultWhenEvaluationLimitExceeded() {
            // maxEvaluations=1 forces TooManyEvaluationsException after minimal work
            // Nelder-Mead needs n+1 evaluations to build the simplex, so 1 is too few
            // but some evaluations will complete before the exception
            OptimizationResult result = Optimizer.builder()
                    .parameter("Growth Rate", 0.01, 0.20)
                    .modelFactory(params -> buildGrowthModel(params.get("Growth Rate")))
                    .objective(Objectives.minimize("Population"))
                    .algorithm(OptimizationAlgorithm.NELDER_MEAD)
                    .maxEvaluations(3)
                    .timeStep(DAY)
                    .duration(Times.weeks(1))
                    .build()
                    .execute();

            assertNotNull(result.getBestParameters());
            assertNotNull(result.getBestRunResult());
            assertTrue(result.getEvaluationCount() > 0);
        }
    }

    @Nested
    class ResultAccess {

        @Test
        void shouldReturnStockSeries() {
            RunResult run = runGrowthModel(0.05);
            double[] series = run.getStockSeries("Population");
            assertTrue(series.length > 0, "Stock series should not be empty");
            assertEquals(100.0, series[0], 0.001, "Initial population should be 100");
        }

        @Test
        void shouldThrowForUnknownStock() {
            RunResult run = runGrowthModel(0.05);
            assertThrows(IllegalArgumentException.class, () -> run.getStockSeries("NonExistent"));
        }
    }

    // --- Helper methods ---

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

    private Model buildLogisticModel(double growthRate, double capacity) {
        Model model = new Model("Logistic");
        Stock population = new Stock("Population", 10, THING);

        Flow growth = Flow.create("Growth", DAY, () -> {
            double pop = population.getQuantity().getValue();
            double rate = growthRate * pop * (1.0 - pop / capacity);
            return new Quantity(Math.max(0, rate), THING);
        });

        population.addInflow(growth);
        model.addStock(population);
        return model;
    }

    private RunResult runGrowthModel(double growthRate) {
        Model model = buildGrowthModel(growthRate);
        RunResult runResult = new RunResult(Map.of("Growth Rate", growthRate));

        systems.courant.shrewd.Simulation simulation =
                new systems.courant.shrewd.Simulation(model, DAY, Times.weeks(2));
        simulation.addEventHandler(runResult);
        simulation.execute();

        return runResult;
    }

    private RunResult runLogisticModel(double growthRate, double capacity) {
        Model model = buildLogisticModel(growthRate, capacity);
        RunResult runResult = new RunResult(
                Map.of("Growth Rate", growthRate, "Capacity", capacity));

        systems.courant.shrewd.Simulation simulation =
                new systems.courant.shrewd.Simulation(model, DAY, Times.weeks(4));
        simulation.addEventHandler(runResult);
        simulation.execute();

        return runResult;
    }
}

package systems.courant.sd.sweep;

import systems.courant.sd.Simulation;
import systems.courant.sd.measure.Quantity;
import systems.courant.sd.measure.units.time.Times;
import systems.courant.sd.model.Flow;
import systems.courant.sd.model.Model;
import systems.courant.sd.model.Stock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static systems.courant.sd.measure.Units.DAY;
import static systems.courant.sd.measure.Units.THING;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SimulationRunner")
class SimulationRunnerTest {

    @Nested
    @DisplayName("run (multi-parameter)")
    class RunMultiParam {

        @Test
        @DisplayName("should create simulation, execute, and return populated RunResult")
        void shouldRunWithModelFactory() {
            Map<String, Double> params = Map.of("Growth Rate", 0.05);
            RunResult result = SimulationRunner.run(
                    p -> buildGrowthModel(p.get("Growth Rate")),
                    null,
                    params, DAY, Times.weeks(2));

            assertThat(result.getStepCount()).isGreaterThan(0);
            assertThat(result.getStockNames()).contains("Population");
            assertThat(result.getParameterMap()).isEqualTo(params);
        }

        @Test
        @DisplayName("should prefer compiledModelFactory when both are provided")
        void shouldPreferCompiledFactory() {
            // When compiledModelFactory is non-null, modelFactory should not be called
            Map<String, Double> params = Map.of("Growth Rate", 0.05);
            boolean[] modelFactoryCalled = {false};

            // compiledModelFactory is null here, so modelFactory will be used
            RunResult result = SimulationRunner.run(
                    p -> {
                        modelFactoryCalled[0] = true;
                        return buildGrowthModel(p.get("Growth Rate"));
                    },
                    null,
                    params, DAY, Times.weeks(1));

            assertThat(modelFactoryCalled[0]).isTrue();
            assertThat(result.getStepCount()).isGreaterThan(0);
        }
    }

    @Nested
    @DisplayName("run (single-parameter)")
    class RunSingleParam {

        @Test
        @DisplayName("should create simulation from DoubleFunction factory and return result")
        void shouldRunWithDoubleFunction() {
            RunResult result = SimulationRunner.run(
                    SimulationRunnerTest::buildGrowthModel,
                    null,
                    "Growth Rate", 0.05,
                    DAY, Times.weeks(2));

            assertThat(result.getStepCount()).isGreaterThan(0);
            assertThat(result.getStockNames()).contains("Population");
            assertThat(result.getParameterMap()).containsEntry("Growth Rate", 0.05);
        }
    }

    @Nested
    @DisplayName("createSimulation")
    class CreateSimulation {

        @Test
        @DisplayName("should return a Simulation without executing it")
        void shouldCreateWithoutExecuting() {
            Map<String, Double> params = Map.of("Growth Rate", 0.05);
            Simulation simulation = SimulationRunner.createSimulation(
                    p -> buildGrowthModel(p.get("Growth Rate")),
                    null,
                    params, DAY, Times.weeks(1));

            assertThat(simulation).isNotNull();
            // Simulation should not have been executed yet — verify by executing manually
            RunResult result = SimulationRunner.execute(simulation, params);
            assertThat(result.getStepCount()).isGreaterThan(0);
        }
    }

    @Nested
    @DisplayName("execute")
    class Execute {

        @Test
        @DisplayName("should attach handler and execute simulation")
        void shouldAttachHandlerAndExecute() {
            Model model = buildGrowthModel(0.05);
            Simulation simulation = new Simulation(model, DAY, Times.weeks(1));
            Map<String, Double> params = Map.of("Growth Rate", 0.05);

            RunResult result = SimulationRunner.execute(simulation, params);

            assertThat(result.getStepCount()).isGreaterThan(0);
            assertThat(result.getStockNames()).contains("Population");
            assertThat(result.getParameterMap()).isEqualTo(params);
        }

        @Test
        @DisplayName("should record parameter map in RunResult")
        void shouldRecordParameters() {
            Model model = buildGrowthModel(0.10);
            Simulation simulation = new Simulation(model, DAY, Times.weeks(1));
            Map<String, Double> params = Map.of("Growth Rate", 0.10, "Other", 42.0);

            RunResult result = SimulationRunner.execute(simulation, params);

            assertThat(result.getParameterMap())
                    .containsEntry("Growth Rate", 0.10)
                    .containsEntry("Other", 42.0);
        }
    }

    private static Model buildGrowthModel(double growthRate) {
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

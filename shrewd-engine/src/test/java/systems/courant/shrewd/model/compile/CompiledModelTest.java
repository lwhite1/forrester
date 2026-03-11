package systems.courant.shrewd.model.compile;

import systems.courant.shrewd.Simulation;
import systems.courant.shrewd.measure.Quantity;
import systems.courant.shrewd.measure.UnitRegistry;
import systems.courant.shrewd.model.Flow;
import systems.courant.shrewd.model.Model;
import systems.courant.shrewd.model.Stock;
import systems.courant.shrewd.model.Variable;
import systems.courant.shrewd.model.def.FlowDef;
import systems.courant.shrewd.model.def.ModelDefinition;
import systems.courant.shrewd.model.def.SimulationSettings;
import systems.courant.shrewd.model.def.StockDef;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static systems.courant.shrewd.measure.Units.MINUTE;
import static systems.courant.shrewd.measure.Units.THING;

@DisplayName("CompiledModel")
class CompiledModelTest {

    private Model model;
    private Stock population;
    private CompiledModel compiled;
    private int[] stepHolder;
    private double[] dtHolder;
    private AtomicInteger resetCount;

    @BeforeEach
    void setUp() {
        model = new Model("TestModel");
        population = new Stock("Population", 100, THING);
        Flow growth = Flow.create("Growth", MINUTE,
                () -> new Quantity(population.getValue() * 0.1, THING));
        population.addInflow(growth);
        model.addStock(population);
        model.addFlow(growth);

        stepHolder = new int[]{0};
        dtHolder = new double[]{1.0};
        resetCount = new AtomicInteger(0);

        ModelDefinition source = new ModelDefinition(
                "TestModel", null, null,
                List.of(new StockDef("Population", 100, "things")),
                List.of(new FlowDef("Growth", "Population * 0.1", "Minute", null, "Population")),
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(),
                new SimulationSettings("Minute", 10.0, "Minute"),
                null);

        compiled = new CompiledModel(model, List.of(resetCount::incrementAndGet),
                source, stepHolder, dtHolder, new systems.courant.shrewd.measure.TimeUnit[1],
                new UnitRegistry());
    }

    @Nested
    @DisplayName("Getters")
    class Getters {

        @Test
        void shouldReturnModel() {
            assertThat(compiled.getModel()).isSameAs(model);
        }

        @Test
        void shouldReturnSource() {
            assertThat(compiled.getSource().name()).isEqualTo("TestModel");
        }

        @Test
        void shouldReturnResettables() {
            assertThat(compiled.getResettables()).hasSize(1);
        }

        @Test
        void shouldReturnImmutableResettablesList() {
            assertThatThrownBy(() -> compiled.getResettables().add(() -> {}))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("DT management")
    class DtManagement {

        @Test
        void shouldDefaultDtToOne() {
            assertThat(compiled.getDt()).isEqualTo(1.0);
        }

        @Test
        void shouldSetAndGetDt() {
            compiled.setDt(0.25);
            assertThat(compiled.getDt()).isEqualTo(0.25);
        }

        @Test
        void shouldReflectDtInHolder() {
            compiled.setDt(0.5);
            assertThat(dtHolder[0]).isEqualTo(0.5);
        }
    }

    @Nested
    @DisplayName("reset")
    class Reset {

        @Test
        void shouldResetStepHolder() {
            stepHolder[0] = 42;
            compiled.reset();
            assertThat(stepHolder[0]).isZero();
        }

        @Test
        void shouldCallResettables() {
            compiled.reset();
            assertThat(resetCount.get()).isEqualTo(1);
        }

        @Test
        void shouldRestoreInitialStockValues() {
            population.setValue(999.0);
            compiled.reset();
            assertThat(population.getValue()).isEqualTo(100.0);
        }

        @Test
        void shouldAllowMultipleResets() {
            population.setValue(500.0);
            compiled.reset();
            assertThat(population.getValue()).isEqualTo(100.0);

            population.setValue(200.0);
            compiled.reset();
            assertThat(population.getValue()).isEqualTo(100.0);
            assertThat(resetCount.get()).isEqualTo(2);
        }

        @Test
        void shouldClearFlowHistory() {
            Flow growth = model.getFlows().get(0);
            growth.recordValue(new Quantity(10, THING));
            growth.recordValue(new Quantity(20, THING));
            assertThat(growth.getHistoryAtTimeStep(0)).isEqualTo(10.0);
            assertThat(growth.getHistoryAtTimeStep(1)).isEqualTo(20.0);

            compiled.reset();

            // After reset, history should be cleared — returns 0 for out-of-range
            assertThat(growth.getHistoryAtTimeStep(0)).isEqualTo(0.0);
            assertThat(growth.getHistoryAtTimeStep(1)).isEqualTo(0.0);
        }

        @Test
        void shouldClearVariableHistory() {
            Variable rate = new Variable("rate", THING, () -> 0.1);
            model.addVariable(rate);
            rate.recordValue();
            assertThat(rate.getHistoryAtTimeStep(0)).isEqualTo(0.1);

            compiled.reset();

            // After reset, history should be cleared — returns 0 for out-of-range
            assertThat(rate.getHistoryAtTimeStep(0)).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("createSimulation")
    class CreateSimulation {

        @Test
        void shouldCreateSimulationWithExplicitParams() {
            Simulation sim = compiled.createSimulation(MINUTE, 5.0, MINUTE);
            assertThat(sim).isNotNull();
            assertThat(sim.getModel()).isSameAs(model);
        }

        @Test
        void shouldCreateSimulationWithQuantityDuration() {
            Simulation sim = compiled.createSimulation(MINUTE, new Quantity(5.0, MINUTE));
            assertThat(sim).isNotNull();
        }

        @Test
        void shouldCreateSimulationFromDefaults() {
            Simulation sim = compiled.createSimulation();
            assertThat(sim).isNotNull();
        }

        @Test
        void shouldThrowIfNoDefaultSettings() {
            ModelDefinition noDefaults = new ModelDefinition(
                    "NoDefaults", null, null,
                    List.of(), List.of(), List.of(), List.of(),
                    List.of(), List.of(), List.of(), List.of(),
                    List.of(), null, null);
            CompiledModel noSettingsModel = new CompiledModel(
                    model, List.of(), noDefaults, stepHolder, dtHolder,
                    new systems.courant.shrewd.measure.TimeUnit[1], new UnitRegistry());

            assertThatThrownBy(noSettingsModel::createSimulation)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("No default simulation settings");
        }

        @Test
        void shouldSyncStepCounterDuringSimulation() {
            stepHolder[0] = 0;
            Simulation sim = compiled.createSimulation(MINUTE, 3.0, MINUTE);
            sim.execute();
            assertThat(stepHolder[0]).isGreaterThan(0);
        }
    }

    @Nested
    @DisplayName("Reset and re-simulate")
    class ResetAndReSimulate {

        @Test
        void shouldProduceConsistentResultsAfterReset() {
            Simulation sim1 = compiled.createSimulation(MINUTE, 5.0, MINUTE);
            sim1.execute();
            double finalValue1 = population.getValue();

            compiled.reset();

            Simulation sim2 = compiled.createSimulation(MINUTE, 5.0, MINUTE);
            sim2.execute();
            double finalValue2 = population.getValue();

            assertThat(finalValue2).isCloseTo(finalValue1, within(1e-9));
        }
    }
}

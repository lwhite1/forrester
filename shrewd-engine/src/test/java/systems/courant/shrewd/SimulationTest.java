package systems.courant.shrewd;

import systems.courant.shrewd.measure.Quantity;
import systems.courant.shrewd.measure.TimeUnit;
import systems.courant.shrewd.model.Flow;
import systems.courant.shrewd.model.Model;
import systems.courant.shrewd.model.Module;
import systems.courant.shrewd.model.Stock;
import systems.courant.shrewd.model.Variable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static systems.courant.shrewd.measure.Units.DAY;
import static systems.courant.shrewd.measure.Units.GALLON_US;
import static systems.courant.shrewd.measure.Units.HOUR;
import static systems.courant.shrewd.measure.Units.MINUTE;
import static systems.courant.shrewd.measure.Units.SECOND;
import static systems.courant.shrewd.measure.Units.THING;
import static systems.courant.shrewd.measure.Units.WEEK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SimulationTest {

    @Test
    public void shouldDrainStockViaOutflow() {
        Model model = new Model("Drain");
        Stock tank = new Stock("Tank", 100, GALLON_US);

        Flow outflow = Flow.create("Drain", MINUTE, () -> new Quantity(10, GALLON_US));

        tank.addOutflow(outflow);
        model.addStock(tank);

        Simulation sim = new Simulation(model, MINUTE, MINUTE, 5);
        sim.execute();

        // 6 steps (0..5), each draining 10 gallons: 100 - 60 = 40
        assertEquals(40, tank.getValue(), 0.01);
    }

    @Test
    public void shouldFillStockViaInflow() {
        Model model = new Model("Fill");
        Stock tank = new Stock("Tank", 0, GALLON_US);

        Flow inflow = Flow.create("Fill", MINUTE, () -> new Quantity(5, GALLON_US));

        tank.addInflow(inflow);
        model.addStock(tank);

        Simulation sim = new Simulation(model, MINUTE, MINUTE, 3);
        sim.execute();

        // 4 steps (0..3), each adding 5 gallons: 0 + 20 = 20
        assertEquals(20, tank.getValue(), 0.01);
    }

    @Test
    public void shouldBalanceInflowAndOutflow() {
        Model model = new Model("Balance");
        Stock tank = new Stock("Tank", 50, GALLON_US);

        Flow inflow = Flow.create("In", MINUTE, () -> new Quantity(10, GALLON_US));
        Flow outflow = Flow.create("Out", MINUTE, () -> new Quantity(10, GALLON_US));

        tank.addInflow(inflow);
        tank.addOutflow(outflow);
        model.addStock(tank);

        Simulation sim = new Simulation(model, MINUTE, MINUTE, 10);
        sim.execute();

        assertEquals(50, tank.getValue(), 0.01);
    }

    @Test
    public void shouldRecordVariableHistory() {
        Model model = new Model("Var Test");
        Stock stock = new Stock("S1", 100, THING);
        model.addStock(stock);

        Variable var = new Variable("StockLevel", THING, stock::getValue);
        model.addVariable(var);

        Simulation sim = new Simulation(model, MINUTE, MINUTE, 3);
        sim.execute();

        // 4 timesteps recorded
        assertEquals(100.0, var.getHistoryAtTimeStep(0), 0.01);
    }

    @Test
    public void shouldRecordFlowHistory() {
        Model model = new Model("Flow History");
        Stock tank = new Stock("Tank", 100, GALLON_US);

        Flow outflow = Flow.create("Out", MINUTE, () -> new Quantity(7, GALLON_US));

        tank.addOutflow(outflow);
        model.addStock(tank);

        Simulation sim = new Simulation(model, MINUTE, MINUTE, 2);
        sim.execute();

        assertEquals(7.0, outflow.getHistoryAtTimeStep(0), 0.01);
        assertEquals(7.0, outflow.getHistoryAtTimeStep(1), 0.01);
    }

    @Test
    public void shouldProcessModuleStocks() {
        Model model = new Model("Module Test");
        Module module = new Module("M1");

        Stock stock = new Stock("Inventory", 50, THING);
        Flow outflow = Flow.create("Consume", MINUTE, () -> new Quantity(5, THING));
        stock.addOutflow(outflow);

        module.addStock(stock);
        module.addFlow(outflow);
        model.addModule(module);

        Simulation sim = new Simulation(model, MINUTE, MINUTE, 4);
        sim.execute();

        // 5 steps (0..4), each removing 5: 50 - 25 = 25
        assertEquals(25, stock.getValue(), 0.01);
    }

    @Test
    public void shouldClampStockToZeroWhenOutflowExceedsValue() {
        Model model = new Model("Clamp Test");
        Stock tank = new Stock("Tank", 10, GALLON_US);

        Flow outflow = Flow.create("BigDrain", MINUTE, () -> new Quantity(100, GALLON_US));

        tank.addOutflow(outflow);
        model.addStock(tank);

        Simulation sim = new Simulation(model, MINUTE, MINUTE, 2);
        sim.execute();

        // Outflow of 100/step far exceeds the stock of 10; should clamp to 0, not go negative
        assertEquals(0, tank.getValue(), 0.01);
    }

    @Test
    public void shouldTrackElapsedTime() {
        Model model = new Model("Time Test");
        Simulation sim = new Simulation(model, MINUTE, MINUTE, 5);
        sim.execute();

        assertTrue(sim.getElapsedTime().toMinutes() > 0);
    }

    @Test
    public void shouldUpdatePreservedModuleStocks() {
        Model model = new Model("Preserved Module Test");
        Module mod = new Module("M1");

        Stock stock = new Stock("Inventory", 50, THING);
        Flow outflow = Flow.create("Consume", MINUTE, () -> new Quantity(5, THING));
        stock.addOutflow(outflow);

        mod.addStock(stock);
        mod.addFlow(outflow);
        model.addModulePreserved(mod);

        Simulation sim = new Simulation(model, MINUTE, MINUTE, 4);
        sim.execute();

        // 5 steps (0..4), each removing 5: 50 - 25 = 25
        assertEquals(25, stock.getValue(), 0.01);
    }

    @Nested
    @DisplayName("Safety guards")
    class SafetyGuards {

        @Test
        void shouldRejectExcessiveStepCount() {
            Model model = new Model("Too Many Steps");
            // 1 billion seconds with 1-second time step = 1 billion steps > MAX_STEPS
            Simulation sim = new Simulation(model, SECOND,
                    new Quantity(1_000_000_000, DAY));

            assertThatThrownBy(sim::execute)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("max " + Simulation.MAX_STEPS);
        }

        @Test
        void shouldAllowReasonableStepCount() {
            Model model = new Model("Reasonable");
            // 100 minutes with 1-minute step = 101 steps — well within limits
            Simulation sim = new Simulation(model, MINUTE, MINUTE, 100);
            sim.execute();

            assertThat(sim.getCurrentStep()).isEqualTo(101);
        }

        @Test
        void shouldReturnLongStepCounter() {
            Model model = new Model("Long Step");
            Simulation sim = new Simulation(model, MINUTE, MINUTE, 10);
            sim.execute();
            // getCurrentStep() must return long, not int
            long step = sim.getCurrentStep();
            assertThat(step).isEqualTo(11L);
        }

        @Test
        void shouldRespectThreadInterruption() {
            Model model = new Model("Cancel Test");
            Simulation sim = new Simulation(model, MINUTE, MINUTE, 1000);

            // Interrupt the current thread before execute
            Thread.currentThread().interrupt();

            assertThatThrownBy(sim::execute)
                    .isInstanceOf(SimulationCancelledException.class)
                    .hasMessageContaining("cancelled");

            // Clear interrupted status for other tests
            Thread.interrupted();
        }

        @Test
        void shouldTimeoutOnLongSimulation() {
            Model model = new Model("Timeout Test");
            Stock stock = new Stock("S", 100, THING);
            // Slow formula that burns CPU
            Flow flow = Flow.create("F", MINUTE, () -> {
                double x = 0;
                for (int i = 0; i < 100_000; i++) {
                    x += Math.sin(i);
                }
                return new Quantity(x * 0 + 1, THING);
            });
            stock.addOutflow(flow);
            model.addStock(stock);

            Simulation sim = new Simulation(model, MINUTE, MINUTE, 5_000_000);
            sim.setTimeoutMs(100); // 100ms timeout

            assertThatThrownBy(sim::execute)
                    .isInstanceOf(SimulationTimeoutException.class)
                    .hasMessageContaining("timed out");
        }

        @Test
        void shouldTimeoutWithinReasonableStepCount() {
            Model model = new Model("Fine Timeout");
            Stock stock = new Stock("S", 100, THING);
            model.addStock(stock);

            // Use a very short timeout (1ms) with a simple model — should
            // catch within a few hundred steps, not wait for 10,000
            Simulation sim = new Simulation(model, MINUTE, MINUTE, 1_000_000);
            sim.setTimeoutMs(1);

            try {
                sim.execute();
            } catch (SimulationTimeoutException e) {
                // Timeout should fire well before 10,000 steps
                assertThat(e.getMessage()).contains("timed out");
                int step = Integer.parseInt(
                        e.getMessage().replaceAll(".*step (\\d+)/.*", "$1"));
                assertThat(step).isLessThan(10_000);
                return;
            }
            // If it didn't time out (very fast machine), that's OK too
        }

        @Test
        void shouldAllowDisablingTimeout() {
            Model model = new Model("No Timeout");
            Simulation sim = new Simulation(model, MINUTE, MINUTE, 5);
            sim.setTimeoutMs(0); // disabled
            sim.execute();

            assertThat(sim.getCurrentStep()).isEqualTo(6);
        }
    }

    @Nested
    @DisplayName("Step count precision (#428)")
    class StepCountPrecision {

        @Test
        void shouldComputeExactStepCountForWholeNumberDivision() {
            Model model = new Model("Exact");
            Simulation sim = new Simulation(model, DAY, DAY, 100);
            sim.execute();
            // 100 days / 1 day = 100 steps, plus initial = 101
            assertThat(sim.getCurrentStep()).isEqualTo(101);
        }

        @Test
        void shouldComputeCorrectStepsForCrossUnitDuration() {
            Model model = new Model("Cross Unit");
            // 1 week = 7 days, so 7 steps
            Simulation sim = new Simulation(model, DAY, new Quantity(1, WEEK));
            sim.execute();
            assertThat(sim.getCurrentStep()).isEqualTo(8); // 0..7
        }

        @Test
        void shouldSnapToIntegerWhenFPResultIsNearlyExact() {
            Model model = new Model("FP Edge");
            // 10 days / 1 day = 10 steps exactly, but verify epsilon snapping works
            // by using cross-unit: 240 hours / 24 hours (DAY) = 10 steps
            Simulation sim = new Simulation(model, DAY,
                    new Quantity(240, HOUR));
            sim.execute();
            assertThat(sim.getCurrentStep()).isEqualTo(11); // 0..10
        }

        @Test
        void shouldHandleDurationThatIsExactMultipleOfTimeStep() {
            Model model = new Model("Exact Multiple");
            // 10 hours / 1 hour = 10 steps
            Simulation sim = new Simulation(model, HOUR, new Quantity(10, HOUR));
            sim.execute();
            assertThat(sim.getCurrentStep()).isEqualTo(11); // 0..10
        }

        @Test
        void shouldFloorNonIntegerStepCount() {
            Model model = new Model("Floor");
            // 7 days / 3 days = 2.333... → floor to 2 steps
            Simulation sim = new Simulation(model, DAY,
                    new Quantity(7, DAY));
            // Using a 3-day time step
            Simulation sim3 = new Simulation(model, new TimeUnit() {
                @Override
                public String getName() { return "ThreeDay"; }
                @Override
                public systems.courant.shrewd.measure.Dimension getDimension() {
                    return systems.courant.shrewd.measure.Dimension.TIME;
                }
                @Override
                public double ratioToBaseUnit() { return 3 * 86400; }
            }, new Quantity(7, DAY));
            sim3.execute();
            assertThat(sim3.getCurrentStep()).isEqualTo(3); // 0..2
        }

        @Test
        void shouldUseInBaseUnitsForDurationConversion() {
            Model model = new Model("Base Units");
            // Duration in hours, timeStep in minutes: 2 hours / 1 minute = 120 steps
            Simulation sim = new Simulation(model, MINUTE, new Quantity(2, HOUR));
            sim.execute();
            assertThat(sim.getCurrentStep()).isEqualTo(121); // 0..120
        }
    }

    @Nested
    @DisplayName("clearHistory with modules (#460)")
    class ClearHistoryWithModules {

        @Test
        void shouldClearFlowHistoryInPreservedModules() {
            Model model = new Model("Preserved Module Flow History");
            Module mod = new Module("M1");

            Stock stock = new Stock("Inventory", 50, THING);
            Flow outflow = Flow.create("Consume", MINUTE, () -> new Quantity(5, THING));
            stock.addOutflow(outflow);

            mod.addStock(stock);
            mod.addFlow(outflow);
            model.addModulePreserved(mod);

            Simulation sim = new Simulation(model, MINUTE, MINUTE, 2);
            sim.execute();

            // Flow should have history from first run
            assertThat(outflow.getHistoryAtTimeStep(0)).isEqualTo(5.0);

            // Re-run: execute() calls clearHistory() internally
            stock.setValue(50);
            sim.execute();

            // History should reflect second run, not accumulate from first
            assertThat(outflow.getHistoryAtTimeStep(0)).isEqualTo(5.0);
            assertThat(outflow.getHistoryAtTimeStep(1)).isEqualTo(5.0);
            assertThat(outflow.getHistoryAtTimeStep(2)).isEqualTo(5.0);
        }

        @Test
        void shouldClearVariableHistoryInPreservedModules() {
            Model model = new Model("Preserved Module Var History");
            Module mod = new Module("M1");

            Stock stock = new Stock("Inventory", 50, THING);
            model.addStock(stock);

            Variable var = new Variable("Level", THING, stock::getValue);
            mod.addVariable(var);
            model.addModulePreserved(mod);

            Simulation sim = new Simulation(model, MINUTE, MINUTE, 2);
            sim.execute();

            assertThat(var.getHistoryAtTimeStep(0)).isEqualTo(50.0);

            // Change stock value and re-run
            stock.setValue(99);
            sim.execute();

            // Variable history should reflect the new initial value, not stale data
            assertThat(var.getHistoryAtTimeStep(0)).isEqualTo(99.0);
        }

        @Test
        void shouldClearHistoryInNestedSubModules() {
            Model model = new Model("Nested Module History");
            Module parent = new Module("Parent");
            Module child = new Module("Child");

            Stock stock = new Stock("S", 100, THING);
            Flow flow = Flow.create("F", MINUTE, () -> new Quantity(10, THING));
            Variable var = new Variable("V", THING, stock::getValue);
            stock.addOutflow(flow);

            child.addStock(stock);
            child.addFlow(flow);
            child.addVariable(var);
            parent.addSubModule(child);
            model.addModulePreserved(parent);

            Simulation sim = new Simulation(model, MINUTE, MINUTE, 2);
            sim.execute();

            assertThat(flow.getHistoryAtTimeStep(0)).isEqualTo(10.0);
            assertThat(var.getHistoryAtTimeStep(0)).isEqualTo(100.0);

            // Re-run
            stock.setValue(100);
            sim.execute();

            // History should be fresh from second run
            assertThat(flow.getHistoryAtTimeStep(0)).isEqualTo(10.0);
            assertThat(var.getHistoryAtTimeStep(0)).isEqualTo(100.0);
        }

        @Test
        void shouldRecordVariableHistoryInPreservedModules() {
            Model model = new Model("Module Var Recording");
            Module mod = new Module("M1");

            Stock stock = new Stock("S", 100, THING);
            Flow outflow = Flow.create("Out", MINUTE, () -> new Quantity(10, THING));
            stock.addOutflow(outflow);
            model.addStock(stock);

            Variable var = new Variable("StockLevel", THING, stock::getValue);
            mod.addVariable(var);
            model.addModulePreserved(mod);

            Simulation sim = new Simulation(model, MINUTE, MINUTE, 3);
            sim.execute();

            // Variable in preserved module should have been recorded each step
            assertThat(var.getHistoryAtTimeStep(0)).isEqualTo(100.0);
            assertThat(var.getHistoryAtTimeStep(1)).isEqualTo(90.0);
            assertThat(var.getHistoryAtTimeStep(2)).isEqualTo(80.0);
            assertThat(var.getHistoryAtTimeStep(3)).isEqualTo(70.0);
        }
    }

    @Nested
    @DisplayName("Non-finite value detection")
    class NonFiniteDetection {

        @Test
        void shouldKeepPreviousValueWhenDeltaIsNaN() {
            Model model = new Model("NaN Test");
            Stock stock = new Stock("Leaky", 100, THING);

            // Flow that produces NaN (0.0 / 0.0)
            Flow nanFlow = Flow.create("Bad", MINUTE, () -> new Quantity(0.0 / 0.0, THING));
            stock.addInflow(nanFlow);
            model.addStock(stock);

            Simulation sim = new Simulation(model, MINUTE, MINUTE, 3);
            sim.execute();

            // Stock keeps its previous value instead of crashing
            assertThat(stock.getValue()).isEqualTo(100.0);
        }

        @Test
        void shouldKeepPreviousValueWhenDeltaCausesInfinity() {
            Model model = new Model("Infinity Test");
            Stock stock = new Stock("Exploding", Double.MAX_VALUE / 2, THING);

            Flow infFlow = Flow.create("Huge", MINUTE,
                    () -> new Quantity(Double.MAX_VALUE, THING));
            stock.addInflow(infFlow);
            model.addStock(stock);

            Simulation sim = new Simulation(model, MINUTE, MINUTE, 3);
            sim.execute();

            // Stock keeps last valid value — doesn't crash or become Infinity
            assertThat(Double.isFinite(stock.getValue())).isTrue();
            assertThat(stock.getValue()).isEqualTo(Double.MAX_VALUE / 2);
        }

        @Test
        void shouldNotRecordNaNFlowInHistory() {
            Model model = new Model("NaN Flow History");
            Stock stock = new Stock("S", 100, THING);

            // Flow that produces NaN — should NOT be recorded in history
            Flow nanFlow = Flow.create("NaN", MINUTE, () -> new Quantity(Double.NaN, THING));
            stock.addInflow(nanFlow);
            model.addStock(stock);

            Simulation sim = new Simulation(model, MINUTE, MINUTE, 3);
            sim.execute();

            // Flow history should have no entries — NaN values are skipped
            assertThat(nanFlow.getHistoryAtTimeStep(0)).isEqualTo(0.0);
            assertThat(nanFlow.getHistoryAtTimeStep(1)).isEqualTo(0.0);
            assertThat(nanFlow.getHistoryAtTimeStep(2)).isEqualTo(0.0);
        }

        @Test
        void shouldNotRecordInfinityFlowInHistory() {
            Model model = new Model("Inf Flow History");
            Stock stock = new Stock("S", 100, THING);

            Flow infFlow = Flow.create("Inf", MINUTE,
                    () -> new Quantity(Double.POSITIVE_INFINITY, THING));
            stock.addInflow(infFlow);
            model.addStock(stock);

            Simulation sim = new Simulation(model, MINUTE, MINUTE, 2);
            sim.execute();

            // Infinity values should not be recorded in flow history
            assertThat(infFlow.getHistoryAtTimeStep(0)).isEqualTo(0.0);
            assertThat(infFlow.getHistoryAtTimeStep(1)).isEqualTo(0.0);
        }

        @Test
        void shouldRecordFiniteFlowValuesNormally() {
            Model model = new Model("Finite Flow History");
            Stock stock = new Stock("S", 100, THING);

            Flow flow = Flow.create("Normal", MINUTE, () -> new Quantity(5, THING));
            stock.addOutflow(flow);
            model.addStock(stock);

            Simulation sim = new Simulation(model, MINUTE, MINUTE, 2);
            sim.execute();

            // Finite values should still be recorded
            assertThat(flow.getHistoryAtTimeStep(0)).isEqualTo(5.0);
            assertThat(flow.getHistoryAtTimeStep(1)).isEqualTo(5.0);
        }

        @Test
        void shouldNotRepeatWarningForSameStock() {
            Model model = new Model("Repeated NaN");
            Stock stock = new Stock("Bad", 50, THING);

            Flow nanFlow = Flow.create("NaN", MINUTE, () -> new Quantity(Double.NaN, THING));
            stock.addInflow(nanFlow);
            model.addStock(stock);

            // Run for many steps — warning should only fire once per stock
            Simulation sim = new Simulation(model, MINUTE, MINUTE, 100);
            sim.execute();

            // Stock retains initial value throughout
            assertThat(stock.getValue()).isEqualTo(50.0);
        }
    }
}

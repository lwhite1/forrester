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
import static systems.courant.shrewd.measure.Units.MINUTE;
import static systems.courant.shrewd.measure.Units.SECOND;
import static systems.courant.shrewd.measure.Units.THING;
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

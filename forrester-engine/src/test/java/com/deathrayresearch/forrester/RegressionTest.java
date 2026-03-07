package com.deathrayresearch.forrester;

import com.deathrayresearch.forrester.event.EventHandler;
import com.deathrayresearch.forrester.event.SimulationEndEvent;
import com.deathrayresearch.forrester.event.SimulationStartEvent;
import com.deathrayresearch.forrester.event.TimeStepEvent;
import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.model.Delay3;
import com.deathrayresearch.forrester.model.Flow;
import com.deathrayresearch.forrester.model.Model;
import com.deathrayresearch.forrester.model.NegativeValuePolicy;
import com.deathrayresearch.forrester.model.Smooth;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.model.Variable;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.deathrayresearch.forrester.measure.Units.DAY;
import static com.deathrayresearch.forrester.measure.Units.GALLON_US;
import static com.deathrayresearch.forrester.measure.Units.MILLISECOND;
import static com.deathrayresearch.forrester.measure.Units.MINUTE;
import static com.deathrayresearch.forrester.measure.Units.THING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression tests for bugs fixed in audit rounds 1 and 2.
 * Each test would fail without the corresponding fix.
 */
public class RegressionTest {

    // --- Simulation engine regressions ---

    @Test
    public void flowShouldBeRecordedExactlyOncePerStep() {
        // Fixed: flow values were recorded once per stock encounter (twice for transfer flows)
        Model model = new Model("Transfer");
        Stock a = new Stock("A", 100, THING);
        Stock b = new Stock("B", 0, THING);
        Flow transfer = Flow.create("Transfer", MINUTE, () -> new Quantity(10, THING));
        a.addOutflow(transfer);
        b.addInflow(transfer);
        model.addStock(a);
        model.addStock(b);

        Simulation sim = new Simulation(model, MINUTE, MINUTE, 3);
        sim.execute();

        // Transfer of 10/step for 4 steps (0..3) = 40 transferred
        assertEquals(60, a.getValue(), 0.01);
        assertEquals(40, b.getValue(), 0.01);
        // History should have exactly 4 entries (one per step), not 8
        assertEquals(10.0, transfer.getHistoryAtTimeStep(0), 0.01);
        assertEquals(10.0, transfer.getHistoryAtTimeStep(3), 0.01);
    }

    @Test
    public void twoFlowsWithSameNameShouldNotShareCache() {
        // Fixed: flow cache was keyed by name, causing collisions
        Model model = new Model("Name Collision");
        Stock a = new Stock("A", 100, THING);
        Stock b = new Stock("B", 100, THING);
        Flow flowA = Flow.create("Rate", MINUTE, () -> new Quantity(10, THING));
        Flow flowB = Flow.create("Rate", MINUTE, () -> new Quantity(20, THING));
        a.addOutflow(flowA);
        b.addOutflow(flowB);
        model.addStock(a);
        model.addStock(b);

        Simulation sim = new Simulation(model, MINUTE, MINUTE, 1);
        sim.execute();

        // A should drain by 10*2=20, B should drain by 20*2=40
        assertEquals(80, a.getValue(), 0.01);
        assertEquals(60, b.getValue(), 0.01);
    }

    @Test
    public void simulationShouldBeReEntrant() {
        // Fixed: second execute() did nothing because state wasn't reset
        Model model = new Model("Re-run");
        Stock tank = new Stock("Tank", 100, GALLON_US);
        Flow drain = Flow.create("Drain", MINUTE, () -> new Quantity(10, GALLON_US));
        tank.addOutflow(drain);
        model.addStock(tank);

        Simulation sim = new Simulation(model, MINUTE, MINUTE, 2);
        sim.execute();
        double afterFirst = tank.getValue();

        // Reset stock and re-run
        tank.setValue(100);
        drain.clearHistory();
        sim.execute();
        double afterSecond = tank.getValue();

        assertEquals(afterFirst, afterSecond, 0.01);
    }

    @Test
    public void simulationEndEventShouldFireOnException() {
        // Fixed: SimulationEndEvent not posted when formula threw
        Model model = new Model("Throw");
        Stock stock = new Stock("S", 100, THING);
        Flow bad = Flow.create("Bad", MINUTE, () -> { throw new RuntimeException("boom"); });
        stock.addOutflow(bad);
        model.addStock(stock);

        Simulation sim = new Simulation(model, MINUTE, MINUTE, 5);
        List<String> events = new ArrayList<>();
        sim.addEventHandler(new EventHandler() {
            @Override
            public void handleTimeStepEvent(TimeStepEvent event) {}

            @Override
            public void handleSimulationStartEvent(SimulationStartEvent event) {
                events.add("start");
            }

            @Override
            public void handleSimulationEndEvent(SimulationEndEvent event) {
                events.add("end");
            }
        });

        assertThrows(RuntimeException.class, sim::execute);
        assertTrue(events.contains("start"));
        assertTrue(events.contains("end"));
    }

    @Test
    public void subSecondTimeStepsShouldAdvanceTime() {
        // Fixed: MILLISECOND (ratio 0.001) was truncated to 0, time never advanced
        Model model = new Model("Millis");
        Stock stock = new Stock("S", 1000, THING);
        model.addStock(stock);

        Simulation sim = new Simulation(model, MILLISECOND, MILLISECOND, 10);
        sim.execute();

        assertTrue(sim.getElapsedTime().toNanos() > 0,
                "Elapsed time should advance with sub-second steps");
    }

    @Test
    public void simulationShouldRejectNonTimeQuantityDuration() {
        // Fixed: no validation that duration is a TIME quantity
        Model model = new Model("Bad Duration");
        assertThrows(IllegalArgumentException.class,
                () -> new Simulation(model, MINUTE, new Quantity(5, GALLON_US)));
    }

    @Test
    public void simulationShouldRejectNullModel() {
        assertThrows(NullPointerException.class,
                () -> new Simulation(null, MINUTE, MINUTE, 5));
    }

    @Test
    public void simulationShouldRejectZeroDuration() {
        Model model = new Model("Zero");
        assertThrows(IllegalArgumentException.class,
                () -> new Simulation(model, MINUTE, MINUTE, 0));
    }

    // --- SD function regressions ---

    @Test
    public void smoothShouldHandleMultiStepGap() {
        // Fixed: Smooth only integrated once regardless of gap size
        int[] step = {0};
        Smooth smooth = Smooth.of(() -> 100, 5, 0, () -> step[0]);

        // Initialize at step 0
        smooth.getCurrentValue();

        // Jump to step 10 — should integrate 10 times, not once
        step[0] = 10;
        double jumped = smooth.getCurrentValue();

        // Compare with sequential steps
        int[] step2 = {0};
        Smooth sequential = Smooth.of(() -> 100, 5, 0, () -> step2[0]);
        sequential.getCurrentValue();
        for (int i = 1; i <= 10; i++) {
            step2[0] = i;
            sequential.getCurrentValue();
        }
        double stepped = sequential.getCurrentValue();

        assertEquals(stepped, jumped, 0.01,
                "Multi-step gap should produce same result as sequential steps");
    }

    @Test
    public void delay3ShouldHandleMultiStepGap() {
        // Fixed: Delay3 only did one Euler pass regardless of skipped steps
        int[] step = {0};
        Delay3 delay = Delay3.of(() -> 100, 6, 0, () -> step[0]);

        delay.getCurrentValue();

        step[0] = 10;
        double jumped = delay.getCurrentValue();

        int[] step2 = {0};
        Delay3 sequential = Delay3.of(() -> 100, 6, 0, () -> step2[0]);
        sequential.getCurrentValue();
        for (int i = 1; i <= 10; i++) {
            step2[0] = i;
            sequential.getCurrentValue();
        }
        double stepped = sequential.getCurrentValue();

        assertEquals(stepped, jumped, 0.01,
                "Multi-step gap should produce same result as sequential steps");
    }

    // --- Stock regressions ---

    @Test
    public void stockShouldRejectNaN() {
        // Fixed: NaN fell through applyPolicy to CLAMP_TO_ZERO
        Stock stock = new Stock("S", 100, THING);
        assertThrows(IllegalArgumentException.class, () -> stock.setValue(Double.NaN));
    }

    @Test
    public void stockShouldRejectInfinity() {
        // Fixed: Infinity passed unchecked
        Stock stock = new Stock("S", 100, THING);
        assertThrows(IllegalArgumentException.class, () -> stock.setValue(Double.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> stock.setValue(Double.NEGATIVE_INFINITY));
    }

    @Test
    public void stockShouldRejectNullUnit() {
        assertThrows(NullPointerException.class, () -> new Stock("S", 100, null));
    }

    @Test
    public void stockShouldRejectNullPolicy() {
        assertThrows(NullPointerException.class,
                () -> new Stock("S", 100, THING, null));
    }

    // --- Quantity regressions ---

    @Test
    public void quantityEqualsShouldWorkAcrossUnits() {
        // 1 km = 1000 m — physical equality
        Quantity km = new Quantity(1, com.deathrayresearch.forrester.measure.Units.KILOMETER);
        Quantity m = new Quantity(1000, com.deathrayresearch.forrester.measure.Units.METER);
        assertEquals(km, m);
        assertEquals(km.hashCode(), m.hashCode());
    }

    // --- Flow regressions ---

    @Test
    public void flowClearHistoryShouldWork() {
        Flow flow = Flow.create("F", MINUTE, () -> new Quantity(10, THING));
        flow.recordValue(new Quantity(10, THING));
        flow.recordValue(new Quantity(20, THING));
        assertEquals(10.0, flow.getHistoryAtTimeStep(0), 0.01);
        flow.clearHistory();
        assertEquals(0.0, flow.getHistoryAtTimeStep(0), 0.01);
    }

    // --- Variable regressions ---

    @Test
    public void variableShouldRejectNullUnit() {
        assertThrows(NullPointerException.class,
                () -> new Variable("V", null, () -> 42));
    }

    @Test
    public void variableShouldRejectNullFormula() {
        assertThrows(NullPointerException.class,
                () -> new Variable("V", THING, null));
    }

    @Test
    public void variableClearHistoryShouldWork() {
        Variable v = new Variable("V", THING, () -> 42);
        v.recordValue();
        v.recordValue();
        assertEquals(42.0, v.getHistoryAtTimeStep(0), 0.01);
        v.clearHistory();
        assertEquals(0.0, v.getHistoryAtTimeStep(0), 0.01);
    }
}

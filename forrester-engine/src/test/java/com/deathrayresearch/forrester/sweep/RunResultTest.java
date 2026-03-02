package com.deathrayresearch.forrester.sweep;

import com.deathrayresearch.forrester.Simulation;
import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.model.Flow;
import com.deathrayresearch.forrester.model.Model;
import com.deathrayresearch.forrester.model.Stock;
import org.junit.jupiter.api.Test;

import static com.deathrayresearch.forrester.measure.Units.GALLON_US;
import static com.deathrayresearch.forrester.measure.Units.MINUTE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RunResultTest {

    @Test
    void shouldCaptureStockNamesFromModel() {
        Model model = new Model("Test");
        model.addStock(new Stock("Tank", 100, GALLON_US));
        model.addStock(new Stock("Reservoir", 50, GALLON_US));

        RunResult result = new RunResult(1.0);
        Simulation sim = new Simulation(model, MINUTE, MINUTE, 2);
        sim.addEventHandler(result);
        sim.execute();

        assertEquals(2, result.getStockNames().size());
        assertEquals("Tank", result.getStockNames().get(0));
        assertEquals("Reservoir", result.getStockNames().get(1));
    }

    @Test
    void shouldRecordStepCountMatchingSimulation() {
        Model model = new Model("Test");
        model.addStock(new Stock("Tank", 100, GALLON_US));

        RunResult result = new RunResult(5.0);
        Simulation sim = new Simulation(model, MINUTE, MINUTE, 3);
        sim.addEventHandler(result);
        sim.execute();

        // Steps 0..3 = 4 steps
        assertEquals(4, result.getStepCount());
        assertEquals(0, result.getStep(0));
        assertEquals(3, result.getStep(3));
    }

    @Test
    void shouldCaptureStockValuesAtEachStep() {
        Model model = new Model("Drain");
        Stock tank = new Stock("Tank", 100, GALLON_US);
        Flow outflow = Flow.create("Drain", MINUTE, () -> new Quantity(10, GALLON_US));
        tank.addOutflow(outflow);
        model.addStock(tank);

        RunResult result = new RunResult(10.0);
        Simulation sim = new Simulation(model, MINUTE, MINUTE, 2);
        sim.addEventHandler(result);
        sim.execute();

        // Step 0: tank=100, step 1: tank=90, step 2: tank=80
        assertEquals(100.0, result.getStockValuesAtStep(0).get(0), 0.01);
        assertEquals(90.0, result.getStockValuesAtStep(1).get(0), 0.01);
        assertEquals(80.0, result.getStockValuesAtStep(2).get(0), 0.01);
    }

    @Test
    void shouldReturnFinalStockValue() {
        Model model = new Model("Drain");
        Stock tank = new Stock("Tank", 100, GALLON_US);
        Flow outflow = Flow.create("Drain", MINUTE, () -> new Quantity(10, GALLON_US));
        tank.addOutflow(outflow);
        model.addStock(tank);

        RunResult result = new RunResult(10.0);
        Simulation sim = new Simulation(model, MINUTE, MINUTE, 5);
        sim.addEventHandler(result);
        sim.execute();

        // TimeStepEvent fires before stock update, so 6 snapshots capture [100,90,80,70,60,50]
        assertEquals(50.0, result.getFinalStockValue("Tank"), 0.01);
    }

    @Test
    void shouldReturnMaxStockValue() {
        Model model = new Model("Fill");
        Stock tank = new Stock("Tank", 0, GALLON_US);
        Flow inflow = Flow.create("Fill", MINUTE, () -> new Quantity(5, GALLON_US));
        tank.addInflow(inflow);
        model.addStock(tank);

        RunResult result = new RunResult(5.0);
        Simulation sim = new Simulation(model, MINUTE, MINUTE, 3);
        sim.addEventHandler(result);
        sim.execute();

        // TimeStepEvent fires before stock update, so 4 snapshots capture [0,5,10,15]
        assertEquals(15.0, result.getMaxStockValue("Tank"), 0.01);
    }

    @Test
    void shouldThrowForUnknownStockName() {
        Model model = new Model("Test");
        model.addStock(new Stock("Tank", 100, GALLON_US));

        RunResult result = new RunResult(1.0);
        Simulation sim = new Simulation(model, MINUTE, MINUTE, 1);
        sim.addEventHandler(result);
        sim.execute();

        assertThrows(IllegalArgumentException.class, () -> result.getFinalStockValue("Unknown"));
        assertThrows(IllegalArgumentException.class, () -> result.getMaxStockValue("Unknown"));
    }

    @Test
    void shouldStoreParameterValue() {
        RunResult result = new RunResult(42.5);
        assertEquals(42.5, result.getParameterValue(), 0.001);
    }
}

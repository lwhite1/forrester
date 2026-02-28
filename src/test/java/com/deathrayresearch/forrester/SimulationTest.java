package com.deathrayresearch.forrester;

import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.model.Flow;
import com.deathrayresearch.forrester.model.Model;
import com.deathrayresearch.forrester.model.Module;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.model.Variable;
import com.deathrayresearch.forrester.model.flows.FlowPerMinute;
import org.junit.Test;

import static com.deathrayresearch.forrester.measure.Units.GALLON_US;
import static com.deathrayresearch.forrester.measure.Units.MINUTE;
import static com.deathrayresearch.forrester.measure.Units.THING;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SimulationTest {

    @Test
    public void shouldDrainStockViaOutflow() {
        Model model = new Model("Drain");
        Stock tank = new Stock("Tank", 100, GALLON_US);

        Flow outflow = new FlowPerMinute("Drain") {
            @Override
            protected Quantity quantityPerMinute() {
                return new Quantity(10, GALLON_US);
            }
        };

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

        Flow inflow = new FlowPerMinute("Fill") {
            @Override
            protected Quantity quantityPerMinute() {
                return new Quantity(5, GALLON_US);
            }
        };

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

        Flow inflow = new FlowPerMinute("In") {
            @Override
            protected Quantity quantityPerMinute() {
                return new Quantity(10, GALLON_US);
            }
        };

        Flow outflow = new FlowPerMinute("Out") {
            @Override
            protected Quantity quantityPerMinute() {
                return new Quantity(10, GALLON_US);
            }
        };

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

        FlowPerMinute outflow = new FlowPerMinute("Out") {
            @Override
            protected Quantity quantityPerMinute() {
                return new Quantity(7, GALLON_US);
            }
        };

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
        Flow outflow = new FlowPerMinute("Consume") {
            @Override
            protected Quantity quantityPerMinute() {
                return new Quantity(5, THING);
            }
        };
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
    public void shouldTrackElapsedTime() {
        Model model = new Model("Time Test");
        Simulation sim = new Simulation(model, MINUTE, MINUTE, 5);
        sim.execute();

        assertTrue(sim.getElapsedTime().toMinutes() > 0);
    }
}

package com.deathrayresearch.forrester;

import com.deathrayresearch.forrester.model.Model;
import com.deathrayresearch.forrester.model.Module;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.model.Variable;
import org.junit.Test;

import static com.deathrayresearch.forrester.measure.Units.DAY;
import static com.deathrayresearch.forrester.measure.Units.DIMENSIONLESS;
import static com.deathrayresearch.forrester.measure.Units.PEOPLE;
import static org.junit.Assert.assertEquals;

public class ModuleVariableRecordingTest {

    @Test
    public void shouldRecordModuleOnlyVariable() {
        Model model = new Model("Module Variable Test");
        Module module = new Module("Test Module");

        Stock stock = new Stock("Population", 100, PEOPLE);
        module.addStock(stock);

        Variable growthRate = new Variable("Growth Rate", PEOPLE, () -> stock.getQuantity().getValue() * 0.5);
        module.addVariable(growthRate);

        model.addModule(module);

        Simulation sim = new Simulation(model, DAY, DAY, 5);
        sim.execute();

        // 6 steps recorded: steps 0 through 5 inclusive
        assertEquals(50.0, growthRate.getHistoryAtTimeStep(0), 0.001);
        assertEquals(50.0, growthRate.getHistoryAtTimeStep(5), 0.001);
        // Step 6 is out of bounds, should return 0
        assertEquals(0.0, growthRate.getHistoryAtTimeStep(6), 0.001);
    }

    @Test
    public void shouldNotDoubleRecordVariableInBothModelAndModule() {
        Model model = new Model("Duplicate Variable Test");
        Module module = new Module("Test Module");

        Variable counter = new Variable("Counter", DIMENSIONLESS, () -> 1.0);
        module.addVariable(counter);
        model.addVariable(counter);
        model.addModule(module);

        Simulation sim = new Simulation(model, DAY, DAY, 3);
        sim.execute();

        // 4 steps (0-3). If recorded once per step, history at step 3 should be 1.0.
        // If double-recorded, step 3 would map to a different index or there would be 8 entries.
        assertEquals(1.0, counter.getHistoryAtTimeStep(0), 0.001);
        assertEquals(1.0, counter.getHistoryAtTimeStep(1), 0.001);
        assertEquals(1.0, counter.getHistoryAtTimeStep(2), 0.001);
        assertEquals(1.0, counter.getHistoryAtTimeStep(3), 0.001);
        // Step 4 should be out of bounds (only 4 entries, not 8)
        assertEquals(0.0, counter.getHistoryAtTimeStep(4), 0.001);
    }

    @Test
    public void shouldRecordModelVariableAsUsual() {
        Model model = new Model("Model Variable Test");

        Variable constant = new Variable("Constant", DIMENSIONLESS, () -> 42.0);
        model.addVariable(constant);

        Simulation sim = new Simulation(model, DAY, DAY, 2);
        sim.execute();

        // 3 steps (0-2)
        assertEquals(42.0, constant.getHistoryAtTimeStep(0), 0.001);
        assertEquals(42.0, constant.getHistoryAtTimeStep(2), 0.001);
        assertEquals(0.0, constant.getHistoryAtTimeStep(3), 0.001);
    }
}

package com.deathrayresearch.dynamics;

import com.deathrayresearch.dynamics.measure.Dimension;
import com.deathrayresearch.dynamics.measure.Quantity;
import com.deathrayresearch.dynamics.measure.units.time.Day;
import com.deathrayresearch.dynamics.measure.units.time.Times;
import com.deathrayresearch.dynamics.measure.Unit;
import com.deathrayresearch.dynamics.measure.dimension.Item;
import com.deathrayresearch.dynamics.model.Flow;
import com.deathrayresearch.dynamics.model.Model;
import com.deathrayresearch.dynamics.model.Stock;
import com.deathrayresearch.dynamics.rate.Rate;
import org.junit.Test;

/**
 *
 */
public class MalthusPopulationTest {

    @Test
    public void testRun1() {
        Model model = new Model("Population with unconstrained growth");

        Quantity<Item> count = new Quantity<>(100, People.getInstance());
        Stock<Item> population = new Stock<>("pop", count);

        Rate<Item> birthRate = timeUnit -> population.getCurrentValue().multiply(0.04);
        Flow births = new Flow("Births", birthRate);

        Rate<Item> deathRate = timeUnit -> population.getCurrentValue().multiply(0.02);
        Flow<Item> deaths = new Flow<>("Deaths", deathRate);

        population.addInflow(births);
        population.addOutflow(deaths);

        model.addStock(population);

        Simulation run = new Simulation(model, Day.getInstance(), Times.weeks(52));
        run.execute();
    }

    private static class People implements Unit<Item> {

        private static final People instance = new People();

        @Override
        public String getName() {
            return "Person";
        }

        @Override
        public Dimension getDimension() {
            return Item.getInstance();
        }

        @Override
        public double ratioToBaseUnit() {
            return 1.0;
        }

        static People getInstance() {
            return instance;
        }
    }
}

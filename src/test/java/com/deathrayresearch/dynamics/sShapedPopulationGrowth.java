package com.deathrayresearch.dynamics;

import com.deathrayresearch.dynamics.measure.Dimension;
import com.deathrayresearch.dynamics.measure.Quantity;
import com.deathrayresearch.dynamics.measure.Unit;
import com.deathrayresearch.dynamics.measure.dimension.Item;
import com.deathrayresearch.dynamics.measure.units.time.Day;
import com.deathrayresearch.dynamics.measure.units.time.Times;
import com.deathrayresearch.dynamics.model.Constant;
import com.deathrayresearch.dynamics.model.Flow;
import com.deathrayresearch.dynamics.model.Model;
import com.deathrayresearch.dynamics.model.Stock;
import com.deathrayresearch.dynamics.rate.Rate;
import org.junit.Test;

/**
 *
 */
public class sShapedPopulationGrowth {

    @Test
    public void testRun1() {
        Model model = new Model("Population with S-Shaped Growth");

        Quantity<Item> count = new Quantity<>(10, People.getInstance());
        Stock<Item> population = new Stock<>("pop", count);

        Quantity<Item> carryingCapacity = new Quantity<>(1000, People.getInstance());

        Constant fractionalNetBirthRate = new Constant("Maximum Fractional Birth Rate", 0.04);

        // Rates of birth and death vary with the relationship of population to carrying capacity
        // This is a Logistic Growth Model
        Rate<Item> birthRate = timeUnit -> {
            double ratio = population.getCurrentValue().getValue() / carryingCapacity.getValue();
            return population.getCurrentValue().multiply(fractionalNetBirthRate.getCurrentValue() * (1 - ratio));
        };

        Flow births = new Flow("Births", birthRate);

        population.addInflow(births);

        model.addStock(population);

        Simulation run = new Simulation(model, Day.getInstance(), Times.weeks(32));
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

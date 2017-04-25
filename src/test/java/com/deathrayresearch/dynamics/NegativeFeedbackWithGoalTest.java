package com.deathrayresearch.dynamics;

import com.deathrayresearch.dynamics.measure.Dimension;
import com.deathrayresearch.dynamics.measure.Quantity;
import com.deathrayresearch.dynamics.measure.Unit;
import com.deathrayresearch.dynamics.measure.dimension.Item;
import com.deathrayresearch.dynamics.measure.units.time.Day;
import com.deathrayresearch.dynamics.measure.units.time.Times;
import com.deathrayresearch.dynamics.model.Flow;
import com.deathrayresearch.dynamics.model.Model;
import com.deathrayresearch.dynamics.model.Stock;
import com.deathrayresearch.dynamics.rate.Rate;
import org.junit.Test;

/**
 *
 */
public class NegativeFeedbackWithGoalTest {

    @Test
    public void testRun1() {
        Model model = new Model("Negative feedback with goal");

        Quantity<Item> count = new Quantity<>(1000, Inventory.getInstance());
        Stock<Item> inventoryOnHand = new Stock<>("on-hand", count);

        Quantity<Item> goal = new Quantity<>(860, Inventory.getInstance());
        double adjustmentTimeInTimeSteps = 8;

        Rate<Item> productionRate = timeUnit -> {
            Quantity<Item> delta = goal.subtract(inventoryOnHand.getCurrentValue());
            return delta.divide(adjustmentTimeInTimeSteps);
        };

        Flow<Item> production = new Flow<>("Production", productionRate);

        inventoryOnHand.addInflow(production);

        model.addStock(inventoryOnHand);

        Simulation run = new Simulation(model, Day.getInstance(), Times.weeks(12));
        run.execute();
    }

    private static class Inventory implements Unit<Item> {

        private static final Inventory instance = new Inventory();

        @Override
        public String getName() {
            return "Units";
        }

        @Override
        public Dimension getDimension() {
            return Item.getInstance();
        }

        @Override
        public double ratioToBaseUnit() {
            return 1.0;
        }

        static Inventory getInstance() {
            return instance;
        }
    }
}

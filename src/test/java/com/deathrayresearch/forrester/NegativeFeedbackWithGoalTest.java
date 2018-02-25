package com.deathrayresearch.forrester;

import com.deathrayresearch.forrester.measure.Dimension;
import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.Unit;
import com.deathrayresearch.forrester.measure.dimension.Item;
import com.deathrayresearch.forrester.measure.units.time.Day;
import com.deathrayresearch.forrester.measure.units.time.Times;
import com.deathrayresearch.forrester.model.Flow;
import com.deathrayresearch.forrester.model.Model;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.rate.RatePerDay;
import com.deathrayresearch.forrester.rate.Rate;
import com.deathrayresearch.forrester.ui.ChartViewer;
import org.junit.Test;

/**
 *
 */
public class NegativeFeedbackWithGoalTest {

    @Test
    public void testRun1() {
        Model model = new Model("Negative feedback with goal");

        Quantity count = new Quantity(1000, Inventory.getInstance());
        Stock inventoryOnHand = new Stock("on-hand", count);

        Quantity goal = new Quantity(860, Inventory.getInstance());
        double adjustmentTimeInTimeSteps = 8;

        Rate productionRate = new RatePerDay("Production rate") {

            @Override
            protected Quantity quantityPerDay() {
                Quantity delta = goal.subtract(inventoryOnHand.getCurrentValue());
                return delta.divide(adjustmentTimeInTimeSteps);
            }
        };

        Flow production = new Flow("Production", productionRate);

        inventoryOnHand.addInflow(production);

        model.addStock(inventoryOnHand);

        Simulation run = new Simulation(model, Day.getInstance(), Times.weeks(12));
        run.addEventHandler(ChartViewer.newInstance(run.getEventBus()));
        run.execute();
    }

    private static class Inventory implements Unit {

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

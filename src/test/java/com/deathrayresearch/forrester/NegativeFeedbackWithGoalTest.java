package com.deathrayresearch.forrester;

import com.deathrayresearch.forrester.measure.Dimension;
import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.Unit;
import com.deathrayresearch.forrester.measure.dimension.Item;
import com.deathrayresearch.forrester.measure.units.time.Day;
import com.deathrayresearch.forrester.model.Model;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.rate.Flow;
import com.deathrayresearch.forrester.rate.FlowPerDay;
import com.deathrayresearch.forrester.ui.ChartViewer;
import org.junit.Test;

import static com.deathrayresearch.forrester.measure.Units.WEEK;

/**
 *
 */
public class NegativeFeedbackWithGoalTest {

    @Test
    public void testRun1() {
        Model model = new Model("Negative feedback with goal");

        Stock inventoryOnHand = new Stock("Inventory on-hand", 1000, Inventory.getInstance());

        Quantity goal = new Quantity(860, Inventory.getInstance());
        double adjustmentTimeInTimeSteps = 8;

        Flow production = new FlowPerDay("Production") {

            @Override
            protected Quantity quantityPerDay() {
                Quantity delta = goal.subtract(inventoryOnHand.getQuantity());
                return delta.divide(adjustmentTimeInTimeSteps);
            }
        };

        inventoryOnHand.addInflow(production);

        model.addStock(inventoryOnHand);

        Simulation run = new Simulation(model, Day.getInstance(), WEEK, 12);
        run.addEventHandler(new ChartViewer());
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

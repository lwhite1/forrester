package com.deathrayresearch.forrester;

import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.Unit;
import com.deathrayresearch.forrester.measure.units.item.ItemUnit;
import com.deathrayresearch.forrester.measure.units.time.TimeUnits;
import com.deathrayresearch.forrester.model.Model;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.model.Flow;
import com.deathrayresearch.forrester.model.flows.FlowPerDay;
import com.deathrayresearch.forrester.ui.StockLevelChartViewer;
import org.junit.Test;

import static com.deathrayresearch.forrester.measure.Units.WEEK;

/**
 *
 */
public class NegativeFeedbackWithGoalTest {

    private static final Unit INVENTORY = new ItemUnit("Units");

    @Test
    public void testRun1() {
        Model model = new Model("Negative feedback with goal");

        Stock inventoryOnHand = new Stock("Inventory on-hand", 1000, INVENTORY);

        Quantity goal = new Quantity(860, INVENTORY);
        double adjustmentTimeInTimeSteps = 8;

        Flow production = new FlowPerDay("Production") {

            @Override
            protected Quantity quantityPerTimeUnit() {
                Quantity delta = goal.subtract(inventoryOnHand.getQuantity());
                return delta.divide(adjustmentTimeInTimeSteps);
            }
        };

        inventoryOnHand.addInflow(production);

        model.addStock(inventoryOnHand);

        Simulation run = new Simulation(model, TimeUnits.DAY, WEEK, 12);
        run.addEventHandler(new StockLevelChartViewer());
        run.execute();
    }
}

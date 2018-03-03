package com.deathrayresearch.forrester;

import com.deathrayresearch.forrester.archetypes.SimpleExponentialChange;
import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.units.item.People;
import com.deathrayresearch.forrester.measure.units.time.Day;
import com.deathrayresearch.forrester.measure.units.time.Times;
import com.deathrayresearch.forrester.model.Flow;
import com.deathrayresearch.forrester.model.Model;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.model.flows.FlowPerDay;
import com.deathrayresearch.forrester.ui.StockLevelChartViewer;
import org.junit.Test;

import static com.deathrayresearch.forrester.measure.Units.PEOPLE;

/**
 *
 */
public class FirstOrderMaterialDelay {

    @Test
    public void testRun1() {

        Model model = new Model("First order material delay");
        model.setComment("A 1st order material delay is basically an exponential decay function. It is based on " +
                "the assumption that the stock is completely mixed, like the water in a tub, so FIFO is not possible. " +
                "The flow is simply the stock level divided by the average delay");

        Stock potentialCustomers = new Stock("Potential Customers", 1000, PEOPLE);

        Flow sales = new FlowPerDay("Sales") {
            @Override
            protected Quantity quantityPerDay() {
                double averageDelay = 120; // 120 days
                return potentialCustomers.getQuantity().divide(averageDelay);
            }
        };

        potentialCustomers.addOutflow(sales);

        model.addStock(potentialCustomers);

        Simulation run = new Simulation(model, Day.getInstance(), Times.weeks( 52));
        run.addEventHandler(new StockLevelChartViewer());
        run.execute();
    }
}

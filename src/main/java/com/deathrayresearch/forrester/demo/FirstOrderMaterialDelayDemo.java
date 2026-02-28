package com.deathrayresearch.forrester.demo;

import com.deathrayresearch.forrester.Simulation;
import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.units.time.TimeUnits;
import com.deathrayresearch.forrester.measure.units.time.Times;
import com.deathrayresearch.forrester.model.Flow;
import com.deathrayresearch.forrester.model.Model;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.model.flows.FlowPerDay;
import com.deathrayresearch.forrester.ui.StockLevelChartViewer;

import static com.deathrayresearch.forrester.measure.Units.PEOPLE;

/**
 * Demonstrates a first-order material delay (exponential smoothing).
 *
 * <p>A Potential Customers stock drains through a sales outflow equal to the stock level divided
 * by an average delay of 120 days. This assumes the stock is fully mixed (no FIFO ordering),
 * producing exponential decay — the simplest material delay in system dynamics.
 */
public class FirstOrderMaterialDelayDemo {

    public static void main(String[] args) {
        new FirstOrderMaterialDelayDemo().run();
    }

    public void run() {

        Model model = new Model("First order material delay");
        model.setComment("A 1st order material delay is basically an exponential decay function. It is based on " +
                "the assumption that the stock is completely mixed, like the water in a tub, so FIFO is not possible. " +
                "The flow is simply the stock level divided by the average delay");

        Stock potentialCustomers = new Stock("Potential Customers", 1000, PEOPLE);

        Flow sales = new FlowPerDay("Sales") {
            @Override
            protected Quantity quantityPerTimeUnit() {
                double averageDelay = 120; // 120 days
                return potentialCustomers.getQuantity().divide(averageDelay);
            }
        };

        potentialCustomers.addOutflow(sales);

        model.addStock(potentialCustomers);

        Simulation run = new Simulation(model, TimeUnits.DAY, Times.weeks( 52));
        run.addEventHandler(new StockLevelChartViewer());
        run.execute();
    }
}

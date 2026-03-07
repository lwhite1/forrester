package com.deathrayresearch.forrester.demo;

import com.deathrayresearch.forrester.Simulation;
import com.deathrayresearch.forrester.measure.units.time.TimeUnits;
import com.deathrayresearch.forrester.measure.units.time.Times;
import com.deathrayresearch.forrester.model.Flow;
import com.deathrayresearch.forrester.model.Model;
import com.deathrayresearch.forrester.model.ModelMetadata;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.ui.StockLevelChartViewer;

import static com.deathrayresearch.forrester.measure.Units.DAY;
import static com.deathrayresearch.forrester.measure.Units.PEOPLE;

/**
 * Demonstrates a first-order material delay (exponential smoothing).
 *
 * <p>A Potential Customers stock drains through a sales outflow equal to the stock level divided
 * by an average delay. This assumes the stock is fully mixed (no FIFO ordering),
 * producing exponential decay — the simplest material delay in system dynamics.
 */
public class FirstOrderMaterialDelayDemo {

    public static void main(String[] args) {
        double initialCustomers = 1000;
        double averageDelayDays = 120;
        double durationWeeks = 52;

        new FirstOrderMaterialDelayDemo().run(initialCustomers, averageDelayDays, durationWeeks);
    }

    public void run(double initialCustomers, double averageDelayDays, double durationWeeks) {
        Model model = new Model("First order material delay");
        model.setMetadata(ModelMetadata.builder()
                .license("CC-BY-SA-4.0")
                .build());

        Stock potentialCustomers = new Stock("Potential Customers", initialCustomers, PEOPLE);

        Flow sales = Flow.create("Sales", DAY, () ->
                potentialCustomers.getQuantity().divide(averageDelayDays));

        potentialCustomers.addOutflow(sales);

        model.addStock(potentialCustomers);

        Simulation run = new Simulation(model, TimeUnits.DAY, Times.weeks(durationWeeks));
        run.addEventHandler(new StockLevelChartViewer());
        run.execute();
    }
}

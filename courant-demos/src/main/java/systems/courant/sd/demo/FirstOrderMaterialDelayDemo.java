/*
 * Copyright (c) 2026 Courant Systems
 * Licensed under CC-BY-SA-4.0. See LICENSE in this module for details.
 */

package systems.courant.sd.demo;

import systems.courant.sd.Simulation;
import systems.courant.sd.measure.units.time.TimeUnits;
import systems.courant.sd.measure.units.time.Times;
import systems.courant.sd.model.Flow;
import systems.courant.sd.model.Model;
import systems.courant.sd.model.ModelMetadata;
import systems.courant.sd.model.Stock;
import systems.courant.sd.ui.StockLevelChartViewer;

import static systems.courant.sd.measure.Units.DAY;
import static systems.courant.sd.measure.Units.PEOPLE;

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

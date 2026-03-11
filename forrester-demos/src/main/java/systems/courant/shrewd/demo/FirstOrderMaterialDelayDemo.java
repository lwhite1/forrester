/*
 * Copyright (c) 2026 Courant Systems
 * Licensed under CC-BY-SA-4.0. See LICENSE in this module for details.
 */

package systems.courant.shrewd.demo;

import systems.courant.shrewd.Simulation;
import systems.courant.shrewd.measure.units.time.TimeUnits;
import systems.courant.shrewd.measure.units.time.Times;
import systems.courant.shrewd.model.Flow;
import systems.courant.shrewd.model.Model;
import systems.courant.shrewd.model.ModelMetadata;
import systems.courant.shrewd.model.Stock;
import systems.courant.shrewd.ui.StockLevelChartViewer;

import static systems.courant.shrewd.measure.Units.DAY;
import static systems.courant.shrewd.measure.Units.PEOPLE;

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

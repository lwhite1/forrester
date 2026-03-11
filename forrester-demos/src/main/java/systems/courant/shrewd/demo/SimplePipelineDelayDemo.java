/*
 * Copyright (c) 2026 Courant Systems
 * Licensed under CC-BY-SA-4.0. See LICENSE in this module for details.
 */

package systems.courant.shrewd.demo;

import systems.courant.shrewd.Simulation;
import systems.courant.shrewd.measure.Quantity;
import systems.courant.shrewd.model.Flow;
import systems.courant.shrewd.model.Flows;
import systems.courant.shrewd.model.Model;
import systems.courant.shrewd.model.ModelMetadata;
import systems.courant.shrewd.model.Stock;
import systems.courant.shrewd.ui.StockLevelChartViewer;

import static systems.courant.shrewd.measure.Units.DAY;
import static systems.courant.shrewd.measure.Units.THING;
import static systems.courant.shrewd.measure.Units.WEEK;

/**
 * Demonstrates a FIFO pipeline delay where output exactly mirrors input after a fixed lag.
 *
 * <p>A WIP stock receives a constant arrival flow. The departure flow replays
 * the arrival history shifted by a delay constant, using the {@code PipelineDelay}
 * archetype. WIP rises during the delay then stabilizes once departures begin.
 */
public class SimplePipelineDelayDemo {

    public static void main(String[] args) {
        double initialWip = 0;
        double arrivalRate = 5;    // items per day
        int delayDays = 3;
        double durationWeeks = 5;

        new SimplePipelineDelayDemo().run(initialWip, arrivalRate, delayDays, durationWeeks);
    }

    public void run(double initialWip, double arrivalRate, int delayDays, double durationWeeks) {
        Model model = new Model("Simple Pipeline Delay");
        model.setMetadata(ModelMetadata.builder()
                .license("CC-BY-SA-4.0")
                .build());
        Simulation run = new Simulation(model, DAY, WEEK, durationWeeks);

        Stock wip = new Stock("WIP", initialWip, THING);

        Flow arrivals = Flows.constant("Arrivals", DAY, new Quantity(arrivalRate, THING));

        Flow departures = Flows.pipelineDelay("Departures", DAY, arrivals,
                () -> (int) run.getCurrentStep(), delayDays);

        wip.addInflow(arrivals);
        wip.addOutflow(departures);
        model.addStock(wip);

        run.addEventHandler(new StockLevelChartViewer());
        run.execute();
    }
}

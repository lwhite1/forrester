package com.deathrayresearch.forrester.demo;

import com.deathrayresearch.forrester.Simulation;
import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.model.Flow;
import com.deathrayresearch.forrester.model.Flows;
import com.deathrayresearch.forrester.model.Model;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.ui.StockLevelChartViewer;

import static com.deathrayresearch.forrester.measure.Units.DAY;
import static com.deathrayresearch.forrester.measure.Units.THING;
import static com.deathrayresearch.forrester.measure.Units.WEEK;

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
        Simulation run = new Simulation(model, DAY, WEEK, durationWeeks);

        Stock wip = new Stock("WIP", initialWip, THING);

        Flow arrivals = Flows.constant("Arrivals", DAY, new Quantity(arrivalRate, THING));

        Flow departures = Flows.pipelineDelay("Departures", DAY, arrivals,
                run::getCurrentStep, delayDays);

        wip.addInflow(arrivals);
        wip.addOutflow(departures);
        model.addStock(wip);

        run.addEventHandler(new StockLevelChartViewer());
        run.execute();
    }
}

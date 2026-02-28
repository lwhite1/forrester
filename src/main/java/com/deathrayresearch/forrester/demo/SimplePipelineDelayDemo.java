package com.deathrayresearch.forrester.demo;

import com.deathrayresearch.forrester.Simulation;
import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.units.time.TimeUnits;
import com.deathrayresearch.forrester.model.Constant;
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
 * <p>A WIP stock receives a constant arrival flow of 5 items/day. The departure flow replays
 * the arrival history shifted by a 3-day delay constant, using the {@code PipelineDelay}
 * archetype. WIP rises for the first 3 days then stabilizes once departures begin.
 */
public class SimplePipelineDelayDemo {

    public static void main(String[] args) {
        new SimplePipelineDelayDemo().run();
    }

    public void run() {

        Model model = new Model("Simple Pipeline Delay");
        model.setComment("This model illustrates a pipeline delay, where a stock (WIP) is bounded by " +
                "arrival and departure flows. The departure flow is calculated by adding a constant " +
                "number of days to the arrival flow");
        Simulation run = new Simulation(model, DAY, WEEK, 5);


        Stock population = new Stock("WIP", 0, THING);
        Constant delay = new Constant("Activity Time", TimeUnits.DAY, 3);

        Flow arrivals = Flows.constant("Arrivals", DAY, new Quantity(5, THING));

        Flow departures = Flows.pipelineDelay("Departures", DAY, arrivals,
                run::getCurrentStep, delay.getIntValue());

        population.addInflow(arrivals);
        population.addOutflow(departures);
        model.addStock(population);

        System.out.println(model.getComment());
        run.addEventHandler(new StockLevelChartViewer());
        run.execute();
    }
}

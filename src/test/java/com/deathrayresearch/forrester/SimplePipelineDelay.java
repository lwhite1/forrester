package com.deathrayresearch.forrester;

import com.deathrayresearch.forrester.archetypes.PipelineDelay;
import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.units.time.Day;
import com.deathrayresearch.forrester.model.Constant;
import com.deathrayresearch.forrester.model.Model;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.model.Flow;
import com.deathrayresearch.forrester.model.flows.FlowPerDay;
import com.deathrayresearch.forrester.ui.StockLevelChartViewer;
import org.junit.Test;

import static com.deathrayresearch.forrester.measure.Units.*;
import static com.deathrayresearch.forrester.measure.Units.WEEK;

public class SimplePipelineDelay {


    @Test
    public void testRun1() {


        Model model = new Model("Simple Pipeline Delay");
        model.setComment("This model illustrates a pipeline delay, where a stock (WIP) is bounded by " +
                "arrival and departure flows. The departure flow is calculated by adding a constant " +
                "number of days to the arrival flow");
        Simulation run = new Simulation(model, DAY, WEEK, 5);


        Stock population = new Stock("WIP", 0, THING);
        Constant delay = new Constant("Activity Time", Day.getInstance(), 3);

        Flow arrivals = new FlowPerDay("Arrivals") {
            @Override
            protected Quantity quantityPerDay() {
                return new Quantity(5, THING);
            }
        };

        Flow departures = new FlowPerDay("Departures") {
            @Override
            protected Quantity quantityPerDay() {
                return PipelineDelay.from(arrivals, run.getCurrentStep(), delay.getIntValue());
            }
        };

        population.addInflow(arrivals);
        population.addOutflow(departures);
        model.addStock(population);

        System.out.println(model.getComment());
        run.addEventHandler(new StockLevelChartViewer());
        run.execute();
    }
}

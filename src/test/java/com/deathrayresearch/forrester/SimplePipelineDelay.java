package com.deathrayresearch.forrester;

import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.units.time.Day;
import com.deathrayresearch.forrester.model.Constant;
import com.deathrayresearch.forrester.model.Model;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.model.Flow;
import com.deathrayresearch.forrester.model.flows.FlowPerDay;
import com.deathrayresearch.forrester.ui.ChartViewer;
import org.junit.Test;

import static com.deathrayresearch.forrester.measure.Units.*;
import static com.deathrayresearch.forrester.measure.Units.WEEK;

public class SimplePipelineDelay {


    @Test
    public void testRun1() {


        Model model = new Model("Simple Pipeline Delay");
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
                int step = run.getCurrentStep();
                int referenceStep = step - Math.toIntExact(Math.round(delay.getValue()));
                double value = arrivals.getHistoryAtTimeStep(referenceStep);
                return new Quantity(value, THING);
            }
        };

        population.addInflow(arrivals);
        population.addOutflow(departures);

        model.addStock(population);

        run.addEventHandler(new ChartViewer());
        run.execute();
    }
}

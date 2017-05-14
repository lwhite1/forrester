package com.deathrayresearch.forrester;

import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.units.item.People;
import com.deathrayresearch.forrester.measure.units.time.Day;
import com.deathrayresearch.forrester.measure.units.time.Times;
import com.deathrayresearch.forrester.model.Flow;
import com.deathrayresearch.forrester.model.Model;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.rate.RatePerDay;
import com.deathrayresearch.forrester.rate.Rate;
import com.deathrayresearch.forrester.ui.ChartViewer;
import org.junit.Test;

/**
 *
 */
public class ExponentialDecayTest {

    @Test
    public void testRun1() {
        Model model = new Model("Population with exponential decay");

        Quantity count = new Quantity(100, People.getInstance());
        Stock population = new Stock("pop", count);

        Rate deathRate = new RatePerDay() {
            @Override
            protected Quantity quantityPerDay() {
                return population.getCurrentValue().divide(80);
            }
        };


        Flow deaths = new Flow("Deaths", deathRate);

        population.addOutflow(deaths);

        model.addStock(population);

        Simulation run = new Simulation(model, Day.getInstance(), Times.weeks(52));
        run.addEventHandler(ChartViewer.newInstance(run.getEventBus()));
        run.execute();
    }
}

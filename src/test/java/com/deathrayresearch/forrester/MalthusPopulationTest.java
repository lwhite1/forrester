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
public class MalthusPopulationTest {

    @Test
    public void testRun1() {
        Model model = new Model("Population with unconstrained growth");

        Stock population = new Stock("population", 100, People.getInstance());

        RatePerDay birthRate = new RatePerDay("Birth rate") {
            @Override
            protected Quantity quantityPerDay() {
                return population.getCurrentValue().multiply(0.04);
            }
        };


        Flow births = new Flow("Births", birthRate);

        Rate deathRate = new RatePerDay("Birth rate") {
            @Override
            protected Quantity quantityPerDay() {
                return population.getCurrentValue().multiply(0.02);
            }
        };

        Flow deaths = new Flow("Deaths", deathRate);

        population.addInflow(births);
        population.addOutflow(deaths);

        model.addStock(population);

        Simulation run = new Simulation(model, Day.getInstance(), Times.weeks(52));
        run.addEventHandler(ChartViewer.newInstance(run.getEventBus()));
        run.execute();
    }
}

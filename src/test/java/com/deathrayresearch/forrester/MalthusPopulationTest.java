package com.deathrayresearch.forrester;

import com.deathrayresearch.forrester.measure.Dimension;
import com.deathrayresearch.forrester.measure.units.time.Day;
import com.deathrayresearch.forrester.measure.units.time.Times;
import com.deathrayresearch.forrester.measure.Unit;
import com.deathrayresearch.forrester.measure.dimension.Item;
import com.deathrayresearch.forrester.model.Flow;
import com.deathrayresearch.forrester.model.Model;
import com.deathrayresearch.forrester.model.Stock;
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

        Rate birthRate = timeUnit -> population.getCurrentValue().multiply(0.04);
        Flow births = new Flow("Births", birthRate);

        Rate deathRate = timeUnit -> population.getCurrentValue().multiply(0.02);
        Flow deaths = new Flow("Deaths", deathRate);

        population.addInflow(births);
        population.addOutflow(deaths);

        model.addStock(population);

        Simulation run = new Simulation(model, Day.getInstance(), Times.weeks(52));
        run.addEventHandler(ChartViewer.newInstance(run.getEventBus()));
        run.execute();
    }

    private static class People implements Unit {

        private static final People instance = new People();

        @Override
        public String getName() {
            return "Person";
        }

        @Override
        public Dimension getDimension() {
            return Item.getInstance();
        }

        @Override
        public double ratioToBaseUnit() {
            return 1.0;
        }

        static People getInstance() {
            return instance;
        }
    }
}

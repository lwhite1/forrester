package com.deathrayresearch.forrester;

import com.deathrayresearch.forrester.measure.Dimension;
import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.Unit;
import com.deathrayresearch.forrester.measure.dimension.Item;
import com.deathrayresearch.forrester.measure.units.time.Day;
import com.deathrayresearch.forrester.measure.units.time.Times;
import com.deathrayresearch.forrester.model.Constant;
import com.deathrayresearch.forrester.model.Flow;
import com.deathrayresearch.forrester.model.Model;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.rate.Rate;
import com.deathrayresearch.forrester.ui.ChartViewer;
import org.junit.Test;

/**
 *
 */
public class sShapedPopulationGrowth {

    @Test
    public void testRun1() {
        Model model = new Model("Population with S-Shaped Growth");

        Stock population = new Stock("pop", 10, People.getInstance());

        Quantity carryingCapacity = new Quantity(1000, People.getInstance());

        Constant fractionalNetBirthRate = new Constant("Maximum Fractional Birth Rate", 0.04);

        // Rates of birth and death vary with the relationship of population to carrying capacity
        // This is a Logistic Growth Model
        Rate birthRate = timeUnit -> {
            double ratio = population.getCurrentValue().getValue() / carryingCapacity.getValue();
            return population.getCurrentValue().multiply(fractionalNetBirthRate.getCurrentValue() * (1 - ratio));
        };

        Flow births = new Flow("Births", birthRate);

        population.addInflow(births);

        model.addStock(population);

        Simulation run = new Simulation(model, Day.getInstance(), Times.weeks(32));
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
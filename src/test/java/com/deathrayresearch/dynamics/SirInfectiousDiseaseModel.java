package com.deathrayresearch.dynamics;

import com.deathrayresearch.dynamics.event.CsvSubscriber;
import com.deathrayresearch.dynamics.measure.Dimension;
import com.deathrayresearch.dynamics.measure.Quantity;
import com.deathrayresearch.dynamics.measure.Unit;
import com.deathrayresearch.dynamics.measure.dimension.Item;
import com.deathrayresearch.dynamics.measure.units.time.Day;
import com.deathrayresearch.dynamics.measure.units.time.Times;
import com.deathrayresearch.dynamics.model.Constant;
import com.deathrayresearch.dynamics.model.Flow;
import com.deathrayresearch.dynamics.model.Model;
import com.deathrayresearch.dynamics.model.Stock;
import com.deathrayresearch.dynamics.rate.Rate;
import com.deathrayresearch.dynamics.ui.ChartViewer;
import org.junit.Test;

/**
 *
 */
public class SirInfectiousDiseaseModel {

    @Test
    public void testRun1() {
        Model model = new Model("SIR Infectious Disease Model");

        Stock<Item> susceptiblePopulation = new Stock<>("Susceptible", 1000, People.getInstance());

        Stock<Item> infectiousPopulation = new Stock<>("Infectious", 10, People.getInstance());

        Stock<Item> recoveredPopulation = new Stock<>("Recovered", 0, People.getInstance());

        // at each step, each susceptible person meets n other people
        Constant contactRate = new Constant("Contact Rate", 8);

        // The number of newly infected at each step, they get moved from susceptible to infectious status.
        Rate<Item> infectiousRate = timeUnit -> {

            Quantity<Item> population = new Quantity<>(1010, People.getInstance());

            double infectiousPortion = infectiousPopulation.getCurrentValue().getValue() / (population.getValue());

            double infectivity = .10; // proportion of encounters with an infectious person results in infection

            double numberOfInfectedMet = contactRate.getCurrentValue() * infectiousPortion;

            double infectedCount = numberOfInfectedMet * susceptiblePopulation.getCurrentValue().getValue() * infectivity;

            if (infectedCount > susceptiblePopulation.getCurrentValue().getValue()) {
                infectedCount = susceptiblePopulation.getCurrentValue().getValue();
            }
            return new Quantity<>(infectedCount, People.getInstance());
        };

        Rate<Item> recoveryRate = timeUnit -> {
            double recoveredProportion = .2; //20% recover per day
            return new Quantity<>(
                    infectiousPopulation.getCurrentValue().getValue()
                            * recoveredProportion,
                    People.getInstance());
        };

        Flow<Item> infected = new Flow<>("Infected", infectiousRate);
        Flow<Item> recovered = new Flow<>("recovered", recoveryRate);

        susceptiblePopulation.addOutflow(infected);
        infectiousPopulation.addInflow(infected);
        infectiousPopulation.addOutflow(recovered);
        recoveredPopulation.addInflow(recovered);

        model.addStock(susceptiblePopulation);
        model.addStock(infectiousPopulation);
        model.addStock(recoveredPopulation);

        Simulation run = new Simulation(model, Day.getInstance(), Times.weeks(8));
        run.addEventHandler(CsvSubscriber.newInstance(run.getEventBus(), "run1.out.csv"));
        run.addEventHandler(ChartViewer.newInstance(run.getEventBus()));
        run.execute();
    }

    private static class People implements Unit<Item> {

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

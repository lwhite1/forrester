package com.deathrayresearch.dynamics;

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
import org.junit.Test;

/**
 *
 */
public class SirInfectiousDiseaseModel {

    @Test
    public void testRun1() {
        Model model = new Model("SIR Infectious Disease Model");

        Quantity<Item> susceptible = new Quantity<>(1000, People.getInstance());
        Stock<Item> susceptiblePopulation = new Stock<>("Susceptible", susceptible);

        Quantity<Item> infectious = new Quantity<>(10, People.getInstance());
        Stock<Item> infectiousPopulation = new Stock<>("Infectious", infectious);

        Quantity<Item> recovering = new Quantity<>(0, People.getInstance());
        Stock<Item> recoveredPopulation = new Stock<>("Recovered", recovering);

        Constant contactRate = new Constant("Contact Rate", 10);

        Rate<Item> infectiousRate = timeUnit -> {

            Quantity<Item> population = susceptible.add(infectious).add(recovering);

            double infectiousPortion = infectious.getValue()/(population.getValue());

            double infectivity = .333; // every third encounter with an infectious person results in infection

            double numberOfInfectedMet = contactRate.getCurrentValue() * infectiousPortion;

            return new Quantity<>(
                    susceptiblePopulation.getCurrentValue().getValue()
                            * infectivity
                            * numberOfInfectedMet,
                    People.getInstance());
        };

        Rate<Item> recoveryRate = timeUnit -> {
            double recoveredProportion = .1; //10% recover per day
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

        Simulation run = new Simulation(model, Day.getInstance(), Times.weeks(20));
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

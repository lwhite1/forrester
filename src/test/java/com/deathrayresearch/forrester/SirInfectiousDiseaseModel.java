package com.deathrayresearch.forrester;

import com.deathrayresearch.forrester.event.CsvSubscriber;
import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.units.item.People;
import com.deathrayresearch.forrester.measure.units.item.Thing;
import com.deathrayresearch.forrester.measure.units.time.Day;
import com.deathrayresearch.forrester.measure.units.time.Times;
import com.deathrayresearch.forrester.model.Constant;
import com.deathrayresearch.forrester.model.Flow;
import com.deathrayresearch.forrester.model.Model;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.rate.Rate;
import com.deathrayresearch.forrester.rate.RatePerDay;
import com.deathrayresearch.forrester.ui.ChartViewer;
import org.junit.Test;

/**
 *
 */
public class SirInfectiousDiseaseModel {

    @Test
    public void testRun1() {
        Model model = new Model("SIR Infectious Disease Model");

        Stock susceptiblePopulation = new Stock("Susceptible", 1000, People.getInstance());

        Stock infectiousPopulation = new Stock("Infectious", 10, People.getInstance());

        Stock recoveredPopulation = new Stock("Recovered", 0, People.getInstance());

        // at each step, each susceptible person meets n other people
        Constant contactRate = new Constant("Contact Rate", Thing.getInstance(), 8);

        // The number of newly infected at each step, they get moved from susceptible to infectious status.
        Rate infectiousRate = new RatePerDay("Infectious rate") {


            @Override
            public Quantity quantityPerDay() {

                Quantity population = new Quantity(1010, People.getInstance());

                double infectiousPortion = infectiousPopulation.getCurrentValue().getValue() / (population.getValue());

                double infectivity = .10; // proportion of encounters with an infectious person results in infection

                double numberOfInfectedMet = contactRate.getCurrentValue() * infectiousPortion;

                double infectedCount = numberOfInfectedMet * susceptiblePopulation.getCurrentValue().getValue() * infectivity;

                if (infectedCount > susceptiblePopulation.getCurrentValue().getValue()) {
                    infectedCount = susceptiblePopulation.getCurrentValue().getValue();
                }
                return new Quantity(infectedCount, People.getInstance());
            }
        };

        Rate recoveryRate = new RatePerDay("Recovery Rate") {

            @Override
            public Quantity quantityPerDay() {
                double recoveredProportion = .2; //20% recover per day
                return new Quantity(infectiousPopulation.getCurrentValue().getValue() * recoveredProportion,
                        People.getInstance());
            }
        };

        Flow infected = new Flow("Infected", infectiousRate);
        Flow recovered = new Flow("Recovered", recoveryRate);

        susceptiblePopulation.addOutflow(infected);
        infectiousPopulation.addInflow(infected);
        infectiousPopulation.addOutflow(recovered);
        recoveredPopulation.addInflow(recovered);

        model.addStock(susceptiblePopulation);
        model.addStock(infectiousPopulation);
        model.addStock(recoveredPopulation);

        Simulation run = new Simulation(model, Day.getInstance(), Times.weeks(8));
        run.addEventHandler(CsvSubscriber.newInstance(run.getEventBus(), "/tmp/forrester/run1out.csv"));
        run.addEventHandler(ChartViewer.newInstance(run.getEventBus()));
        run.execute();
    }
}

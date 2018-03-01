package com.deathrayresearch.forrester;

import com.deathrayresearch.forrester.io.CsvSubscriber;
import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.units.time.Times;
import com.deathrayresearch.forrester.model.Constant;
import com.deathrayresearch.forrester.model.Model;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.rate.Flow;
import com.deathrayresearch.forrester.rate.FlowPerDay;
import com.deathrayresearch.forrester.ui.ChartViewer;
import org.junit.Test;

import static com.deathrayresearch.forrester.measure.Units.*;

/**
 *
 */
public class SirInfectiousDiseaseModel {

    @Test
    public void testRun1() {
        Model model = new Model("SIR Infectious Disease Model");

        Stock susceptiblePopulation = new Stock("Susceptible", 1000, PEOPLE);

        Stock infectiousPopulation = new Stock("Infectious", 10, PEOPLE);

        Stock recoveredPopulation = new Stock("Recovered", 0, PEOPLE);

        // at each step, each susceptible person meets n other people
        Constant contactRate = new Constant("Contact Flow", PEOPLE, 8);

        // The number of newly infected at each step, they get moved from susceptible to infectious status.
        Flow infectiousRate = new FlowPerDay("Infected") {

            @Override
            public Quantity quantityPerDay() {

                double totalPop = susceptiblePopulation.getCurrentValue().getValue()
                        + infectiousPopulation.getCurrentValue().getValue()
                        + recoveredPopulation.getCurrentValue().getValue();

                Quantity population = new Quantity("Total Population", totalPop, PEOPLE);

                double infectiousPortion = infectiousPopulation.getCurrentValue().getValue() / (population.getValue());

                double infectivity = .10; // proportion of encounters with an infectious person results in infection

                double numberOfInfectedMet = contactRate.getCurrentValue() * infectiousPortion;

                double infectedCount = numberOfInfectedMet * susceptiblePopulation.getCurrentValue().getValue() * infectivity;

                if (infectedCount > susceptiblePopulation.getCurrentValue().getValue()) {
                    infectedCount = susceptiblePopulation.getCurrentValue().getValue();
                }
                return new Quantity("Infected", infectedCount, PEOPLE);
            }
        };

        Flow recoveryRate = new FlowPerDay("Recovered") {

            @Override
            public Quantity quantityPerDay() {
                double recoveredProportion = .2; //20% recover per day
                return new Quantity("Recovered", infectiousPopulation.getCurrentValue().getValue() * recoveredProportion,
                        PEOPLE);
            }
        };

        susceptiblePopulation.addOutflow(infectiousRate);
        infectiousPopulation.addInflow(infectiousRate);
        infectiousPopulation.addOutflow(recoveryRate);
        recoveredPopulation.addInflow(recoveryRate);

        model.addStock(susceptiblePopulation);
        model.addStock(infectiousPopulation);
        model.addStock(recoveredPopulation);

        Simulation run = new Simulation(model, DAY, Times.weeks("Simulation duration", 8));
        run.addEventHandler(new CsvSubscriber("/tmp/forrester/run1out.csv"));
        run.addEventHandler(new ChartViewer());
        run.execute();
    }
}

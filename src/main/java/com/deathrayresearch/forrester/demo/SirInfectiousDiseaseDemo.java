package com.deathrayresearch.forrester.demo;

import com.deathrayresearch.forrester.Simulation;
import com.deathrayresearch.forrester.io.CsvSubscriber;
import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.units.time.Times;
import com.deathrayresearch.forrester.model.Constant;
import com.deathrayresearch.forrester.model.Flow;
import com.deathrayresearch.forrester.model.Model;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.ui.StockLevelChartViewer;

import static com.deathrayresearch.forrester.measure.Units.DAY;
import static com.deathrayresearch.forrester.measure.Units.PEOPLE;

/**
 * Implements the classic SIR (Susceptible-Infectious-Recovered) epidemiological model.
 *
 * <p>Three stocks — Susceptible (1,000), Infectious (10), and Recovered (0) — are linked by
 * infection and recovery flows. The infection rate depends on a contact rate, the infectious
 * fraction of the population, and an infectivity constant. The model produces a characteristic
 * epidemic curve: Infectious rises to a peak then falls as the susceptible pool is depleted.
 */
public class SirInfectiousDiseaseDemo {

    public static void main(String[] args) {
        new SirInfectiousDiseaseDemo().run();
    }

    public void run() {
        Model model = new Model("SIR Infectious Disease Model");

        Stock susceptiblePopulation = new Stock("Susceptible", 1000, PEOPLE);

        Stock infectiousPopulation = new Stock("Infectious", 10, PEOPLE);

        Stock recoveredPopulation = new Stock("Recovered", 0, PEOPLE);

        // at each step, each susceptible person meets n other people
        Constant contactRate = new Constant("Contact Flow", PEOPLE, 8);

        // The number of newly infected at each step, they get moved from susceptible to infectious status.
        Flow infectiousRate = Flow.create("Infected", DAY, () -> {
            double totalPop = susceptiblePopulation.getQuantity().getValue()
                    + infectiousPopulation.getQuantity().getValue()
                    + recoveredPopulation.getQuantity().getValue();

            Quantity population = new Quantity(totalPop, PEOPLE);

            double infectiousPortion = infectiousPopulation.getQuantity().getValue() / (population.getValue());

            double infectivity = .10; // proportion of encounters with an infectious person results in infection

            double numberOfInfectedMet = contactRate.getValue() * infectiousPortion;

            double infectedCount = numberOfInfectedMet * susceptiblePopulation.getQuantity().getValue() * infectivity;

            if (infectedCount > susceptiblePopulation.getQuantity().getValue()) {
                infectedCount = susceptiblePopulation.getQuantity().getValue();
            }
            return new Quantity(infectedCount, PEOPLE);
        });

        Flow recoveryRate = Flow.create("Recovered", DAY, () -> {
            double recoveredProportion = .2; //20% recover per day
            return new Quantity(infectiousPopulation.getQuantity().getValue() * recoveredProportion,
                    PEOPLE);
        });

        susceptiblePopulation.addOutflow(infectiousRate);
        infectiousPopulation.addInflow(infectiousRate);
        infectiousPopulation.addOutflow(recoveryRate);
        recoveredPopulation.addInflow(recoveryRate);

        model.addStock(susceptiblePopulation);
        model.addStock(infectiousPopulation);
        model.addStock(recoveredPopulation);

        Simulation run = new Simulation(model, DAY, Times.weeks(8));
        run.addEventHandler(new CsvSubscriber(System.getProperty("java.io.tmpdir") + "/forrester-run1out.csv"));
        run.addEventHandler(new StockLevelChartViewer());
        run.execute();
    }
}

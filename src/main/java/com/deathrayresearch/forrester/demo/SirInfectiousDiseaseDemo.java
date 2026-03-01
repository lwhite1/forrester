package com.deathrayresearch.forrester.demo;

import com.deathrayresearch.forrester.Simulation;
import com.deathrayresearch.forrester.io.CsvSubscriber;
import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.units.time.Times;
import com.deathrayresearch.forrester.model.Flow;
import com.deathrayresearch.forrester.model.Model;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.ui.StockLevelChartViewer;

import static com.deathrayresearch.forrester.measure.Units.DAY;
import static com.deathrayresearch.forrester.measure.Units.PEOPLE;

/**
 * Implements the classic SIR (Susceptible-Infectious-Recovered) epidemiological model.
 *
 * <p>Three stocks — Susceptible, Infectious, and Recovered — are linked by
 * infection and recovery flows. The infection rate depends on a contact rate, the infectious
 * fraction of the population, and an infectivity constant. The model produces a characteristic
 * epidemic curve: Infectious rises to a peak then falls as the susceptible pool is depleted.
 */
public class SirInfectiousDiseaseDemo {

    public static void main(String[] args) {
        double initialSusceptible = 1000;
        double initialInfectious = 10;
        double initialRecovered = 0;
        double contactRate = 8;            // contacts per person per day
        double infectivity = 0.10;         // fraction of contacts that infect
        double recoveryProportion = 0.20;  // fraction that recover per day
        double durationWeeks = 8;

        new SirInfectiousDiseaseDemo().run(initialSusceptible, initialInfectious,
                initialRecovered, contactRate, infectivity, recoveryProportion, durationWeeks);
    }

    public void run(double initialSusceptible, double initialInfectious,
                    double initialRecovered, double contactRate, double infectivity,
                    double recoveryProportion, double durationWeeks) {
        Model model = new Model("SIR Infectious Disease Model");

        Stock susceptible = new Stock("Susceptible", initialSusceptible, PEOPLE);
        Stock infectious = new Stock("Infectious", initialInfectious, PEOPLE);
        Stock recovered = new Stock("Recovered", initialRecovered, PEOPLE);

        Flow infectionRate = Flow.create("Infected", DAY, () -> {
            double totalPop = susceptible.getValue() + infectious.getValue() + recovered.getValue();
            double infectiousFraction = infectious.getValue() / totalPop;
            double infectedCount = contactRate * infectiousFraction * infectivity
                    * susceptible.getValue();
            if (infectedCount > susceptible.getValue()) {
                infectedCount = susceptible.getValue();
            }
            return new Quantity(infectedCount, PEOPLE);
        });

        Flow recoveryRate = Flow.create("Recovered", DAY, () ->
                new Quantity(infectious.getValue() * recoveryProportion, PEOPLE));

        susceptible.addOutflow(infectionRate);
        infectious.addInflow(infectionRate);
        infectious.addOutflow(recoveryRate);
        recovered.addInflow(recoveryRate);

        model.addStock(susceptible);
        model.addStock(infectious);
        model.addStock(recovered);

        Simulation run = new Simulation(model, DAY, Times.weeks(durationWeeks));
        run.addEventHandler(new CsvSubscriber(
                System.getProperty("java.io.tmpdir") + "/forrester-run1out.csv"));
        run.addEventHandler(new StockLevelChartViewer());
        run.execute();
    }
}

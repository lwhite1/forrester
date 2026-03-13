/*
 * Copyright (c) 2026 Courant Systems
 * Licensed under CC-BY-SA-4.0. See LICENSE in this module for details.
 */

package systems.courant.sd.demo;

import systems.courant.sd.Simulation;
import systems.courant.sd.io.CsvSubscriber;
import systems.courant.sd.measure.Quantity;
import systems.courant.sd.measure.units.time.Times;
import systems.courant.sd.model.Flow;
import systems.courant.sd.model.Model;
import systems.courant.sd.model.ModelMetadata;
import systems.courant.sd.model.Stock;
import systems.courant.sd.ui.StockLevelChartViewer;

import static systems.courant.sd.measure.Units.DAY;
import static systems.courant.sd.measure.Units.PEOPLE;

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
        model.setMetadata(ModelMetadata.builder()
                .source("Kermack & McKendrick SIR model (1927)")
                .license("CC-BY-SA-4.0")
                .build());

        Stock susceptible = new Stock("Susceptible", initialSusceptible, PEOPLE);
        Stock infectious = new Stock("Infectious", initialInfectious, PEOPLE);
        Stock recovered = new Stock("Recovered", initialRecovered, PEOPLE);

        Flow infectionRate = Flow.create("Infected", DAY, () -> {
            double totalPop = susceptible.getValue() + infectious.getValue() + recovered.getValue();
            if (totalPop == 0) {
                return new Quantity(0, PEOPLE);
            }
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
                System.getProperty("java.io.tmpdir") + "/courant-run1out.csv"));
        run.addEventHandler(new StockLevelChartViewer());
        run.execute();
    }
}

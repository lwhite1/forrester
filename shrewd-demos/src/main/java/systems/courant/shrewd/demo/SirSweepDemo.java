/*
 * Copyright (c) 2026 Courant Systems
 * Licensed under CC-BY-SA-4.0. See LICENSE in this module for details.
 */

package systems.courant.shrewd.demo;

import systems.courant.shrewd.measure.Quantity;
import systems.courant.shrewd.measure.units.time.Times;
import systems.courant.shrewd.model.Flow;
import systems.courant.shrewd.model.Model;
import systems.courant.shrewd.model.ModelMetadata;
import systems.courant.shrewd.model.Stock;
import systems.courant.shrewd.sweep.ParameterSweep;
import systems.courant.shrewd.sweep.SweepResult;

import static systems.courant.shrewd.measure.Units.DAY;
import static systems.courant.shrewd.measure.Units.PEOPLE;

/**
 * Demonstrates a parameter sweep on the SIR infectious disease model.
 * Sweeps the contact rate across a range and writes both a time series
 * and summary CSV to the system temp directory.
 */
public class SirSweepDemo {

    public static void main(String[] args) {
        double initialSusceptible = 1000;
        double initialInfectious = 10;
        double initialRecovered = 0;
        double infectivity = 0.10;
        double recoveryProportion = 0.2;
        double contactRateMin = 2.0;
        double contactRateMax = 14.0;
        double contactRateStep = 2.0;
        double durationWeeks = 8;

        new SirSweepDemo().run(initialSusceptible, initialInfectious, initialRecovered,
                infectivity, recoveryProportion,
                contactRateMin, contactRateMax, contactRateStep, durationWeeks);
    }

    public void run(double initialSusceptible, double initialInfectious,
                    double initialRecovered, double infectivity, double recoveryProportion,
                    double contactRateMin, double contactRateMax, double contactRateStep,
                    double durationWeeks) {

        SweepResult result = ParameterSweep.builder()
                .parameterName("Contact Rate")
                .parameterValues(ParameterSweep.linspace(
                        contactRateMin, contactRateMax, contactRateStep))
                .modelFactory(contactRate -> buildSirModel(contactRate,
                        initialSusceptible, initialInfectious, initialRecovered,
                        infectivity, recoveryProportion))
                .timeStep(DAY)
                .duration(Times.weeks(durationWeeks))
                .build()
                .execute();

        String tmpDir = System.getProperty("java.io.tmpdir");
        result.writeTimeSeriesCsv(tmpDir + "/shrewd-sweep-timeseries.csv");
        result.writeSummaryCsv(tmpDir + "/shrewd-sweep-summary.csv");

        System.out.println("Sweep complete: " + result.getRunCount() + " runs");
        System.out.println("Time series CSV: " + tmpDir + "/shrewd-sweep-timeseries.csv");
        System.out.println("Summary CSV:     " + tmpDir + "/shrewd-sweep-summary.csv");
    }

    private Model buildSirModel(double contactRate,
                                double initialSusceptible, double initialInfectious,
                                double initialRecovered,
                                double infectivity, double recoveryProportion) {
        Model model = new Model("SIR Sweep (contact rate=" + contactRate + ")");
        model.setMetadata(ModelMetadata.builder()
                .source("Kermack & McKendrick SIR model (1927)")
                .license("CC-BY-SA-4.0")
                .build());

        Stock susceptible = new Stock("Susceptible", initialSusceptible, PEOPLE);
        Stock infectious = new Stock("Infectious", initialInfectious, PEOPLE);
        Stock recovered = new Stock("Recovered", initialRecovered, PEOPLE);

        Flow infectionRate = Flow.create("Infected", DAY, () -> {
            double totalPop = susceptible.getValue() + infectious.getValue()
                    + recovered.getValue();
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

        return model;
    }
}

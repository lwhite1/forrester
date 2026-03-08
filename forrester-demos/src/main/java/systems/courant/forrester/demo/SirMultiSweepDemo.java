/*
 * Copyright (c) 2026 Courant Systems
 * Licensed under CC-BY-SA-4.0. See LICENSE in this module for details.
 */

package systems.courant.forrester.demo;

import systems.courant.forrester.measure.Quantity;
import systems.courant.forrester.measure.units.time.Times;
import systems.courant.forrester.model.Flow;
import systems.courant.forrester.model.Model;
import systems.courant.forrester.model.ModelMetadata;
import systems.courant.forrester.model.Stock;
import systems.courant.forrester.sweep.MultiParameterSweep;
import systems.courant.forrester.sweep.MultiSweepResult;
import systems.courant.forrester.sweep.ParameterSweep;

import static systems.courant.forrester.measure.Units.DAY;
import static systems.courant.forrester.measure.Units.PEOPLE;

/**
 * Demonstrates multi-parameter sweep on the SIR infectious disease model. Sweeps
 * contact rate and infectivity across their respective ranges, producing one run
 * per combination. Writes time series and summary CSVs to the system temp directory.
 */
public class SirMultiSweepDemo {

    public static void main(String[] args) {
        double initialSusceptible = 1000;
        double initialInfectious = 10;
        double initialRecovered = 0;
        double recoveryProportion = 0.2;
        double contactRateMin = 2.0;
        double contactRateMax = 14.0;
        double contactRateStep = 4.0;
        double[] infectivityValues = {0.05, 0.10, 0.15};
        double durationWeeks = 8;

        new SirMultiSweepDemo().run(initialSusceptible, initialInfectious, initialRecovered,
                recoveryProportion, contactRateMin, contactRateMax, contactRateStep,
                infectivityValues, durationWeeks);
    }

    public void run(double initialSusceptible, double initialInfectious,
                    double initialRecovered, double recoveryProportion,
                    double contactRateMin, double contactRateMax, double contactRateStep,
                    double[] infectivityValues, double durationWeeks) {

        MultiSweepResult result = MultiParameterSweep.builder()
                .parameter("Contact Rate", ParameterSweep.linspace(
                        contactRateMin, contactRateMax, contactRateStep))
                .parameter("Infectivity", infectivityValues)
                .modelFactory(params -> buildSirModel(
                        params.get("Contact Rate"), params.get("Infectivity"),
                        initialSusceptible, initialInfectious, initialRecovered,
                        recoveryProportion))
                .timeStep(DAY)
                .duration(Times.weeks(durationWeeks))
                .build()
                .execute();

        String tmpDir = System.getProperty("java.io.tmpdir");
        String timeSeriesPath = tmpDir + "/forrester-multisweep-timeseries.csv";
        String summaryPath = tmpDir + "/forrester-multisweep-summary.csv";

        result.writeTimeSeriesCsv(timeSeriesPath);
        result.writeSummaryCsv(summaryPath);

        System.out.println("Multi-parameter sweep complete: " + result.getRunCount() + " runs");
        System.out.println("Time series CSV: " + timeSeriesPath);
        System.out.println("Summary CSV:     " + summaryPath);
    }

    private Model buildSirModel(double contactRate, double infectivity,
                                double initialSusceptible, double initialInfectious,
                                double initialRecovered, double recoveryProportion) {
        Model model = new Model("SIR Multi-Sweep");
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

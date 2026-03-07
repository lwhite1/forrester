package com.deathrayresearch.forrester.demo;

import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.units.time.Times;
import com.deathrayresearch.forrester.model.Flow;
import com.deathrayresearch.forrester.model.Model;
import com.deathrayresearch.forrester.model.ModelMetadata;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.sweep.ParameterSweep;
import com.deathrayresearch.forrester.sweep.SweepResult;

import static com.deathrayresearch.forrester.measure.Units.DAY;
import static com.deathrayresearch.forrester.measure.Units.PEOPLE;

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
        result.writeTimeSeriesCsv(tmpDir + "/forrester-sweep-timeseries.csv");
        result.writeSummaryCsv(tmpDir + "/forrester-sweep-summary.csv");

        System.out.println("Sweep complete: " + result.getRunCount() + " runs");
        System.out.println("Time series CSV: " + tmpDir + "/forrester-sweep-timeseries.csv");
        System.out.println("Summary CSV:     " + tmpDir + "/forrester-sweep-summary.csv");
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

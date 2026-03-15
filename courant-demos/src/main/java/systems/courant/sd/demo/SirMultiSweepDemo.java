/*
 * Copyright (c) 2026 Courant Systems
 * Licensed under CC-BY-SA-4.0. See LICENSE in this module for details.
 */

package systems.courant.sd.demo;

import systems.courant.sd.measure.units.time.Times;
import systems.courant.sd.sweep.MultiParameterSweep;
import systems.courant.sd.sweep.MultiSweepResult;
import systems.courant.sd.sweep.ParameterSweep;

import static systems.courant.sd.measure.Units.DAY;

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
                .modelFactory(params -> SirModelBuilder.build("SIR Multi-Sweep",
                        params.get("Contact Rate"), params.get("Infectivity"),
                        initialSusceptible, initialInfectious, initialRecovered,
                        recoveryProportion))
                .timeStep(DAY)
                .duration(Times.weeks(durationWeeks))
                .build()
                .execute();

        String tmpDir = System.getProperty("java.io.tmpdir");
        String timeSeriesPath = tmpDir + "/courant-multisweep-timeseries.csv";
        String summaryPath = tmpDir + "/courant-multisweep-summary.csv";

        result.writeTimeSeriesCsv(timeSeriesPath);
        result.writeSummaryCsv(summaryPath);

        System.out.println("Multi-parameter sweep complete: " + result.getRunCount() + " runs");
        System.out.println("Time series CSV: " + timeSeriesPath);
        System.out.println("Summary CSV:     " + summaryPath);
    }
}

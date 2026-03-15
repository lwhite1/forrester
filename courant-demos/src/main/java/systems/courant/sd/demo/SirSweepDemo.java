/*
 * Copyright (c) 2026 Courant Systems
 * Licensed under CC-BY-SA-4.0. See LICENSE in this module for details.
 */

package systems.courant.sd.demo;

import systems.courant.sd.measure.units.time.Times;
import systems.courant.sd.sweep.ParameterSweep;
import systems.courant.sd.sweep.SweepResult;

import static systems.courant.sd.measure.Units.DAY;

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
                .modelFactory(contactRate -> SirModelBuilder.build(
                        "SIR Sweep (contact rate=" + contactRate + ")",
                        contactRate, infectivity, initialSusceptible, initialInfectious,
                        initialRecovered, recoveryProportion))
                .timeStep(DAY)
                .duration(Times.weeks(durationWeeks))
                .build()
                .execute();

        String tmpDir = System.getProperty("java.io.tmpdir");
        result.writeTimeSeriesCsv(tmpDir + "/courant-sweep-timeseries.csv");
        result.writeSummaryCsv(tmpDir + "/courant-sweep-summary.csv");

        System.out.println("Sweep complete: " + result.getRunCount() + " runs");
        System.out.println("Time series CSV: " + tmpDir + "/courant-sweep-timeseries.csv");
        System.out.println("Summary CSV:     " + tmpDir + "/courant-sweep-summary.csv");
    }
}

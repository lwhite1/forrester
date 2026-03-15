/*
 * Copyright (c) 2026 Courant Systems
 * Licensed under CC-BY-SA-4.0. See LICENSE in this module for details.
 */

package systems.courant.sd.demo;

import systems.courant.sd.measure.Quantity;
import systems.courant.sd.model.Flow;
import systems.courant.sd.model.Model;
import systems.courant.sd.model.ModelMetadata;
import systems.courant.sd.model.Stock;

import static systems.courant.sd.measure.Units.DAY;
import static systems.courant.sd.measure.Units.PEOPLE;

/**
 * Builds the standard SIR (Susceptible-Infectious-Recovered) epidemiological model
 * used across the demo suite.
 *
 * <p>Centralizes the infection/recovery flow logic so that bug fixes and formula
 * changes are applied in one place rather than duplicated across multiple demo files.
 */
public final class SirModelBuilder {

    private SirModelBuilder() {
        // utility class
    }

    /**
     * Computes the number of new infections for one time step.
     *
     * <p>Guards against zero total population and clamps the result so that
     * infections never exceed the current susceptible count.
     *
     * @param contactRate  contacts per person per day
     * @param infectivity  fraction of contacts that transmit infection
     * @param susceptible  current susceptible population
     * @param infectious   current infectious population
     * @param recovered    current recovered population
     * @return non-negative count of new infections, at most {@code susceptible}
     */
    public static double computeNewInfections(double contactRate, double infectivity,
                                              double susceptible, double infectious,
                                              double recovered) {
        double totalPop = susceptible + infectious + recovered;
        if (totalPop == 0) {
            return 0;
        }
        double infectiousFraction = infectious / totalPop;
        double infectedCount = contactRate * infectiousFraction * infectivity * susceptible;
        return Math.min(infectedCount, susceptible);
    }

    /**
     * Builds a complete SIR model with three stocks and two flows.
     *
     * @param modelName          display name for the model
     * @param contactRate        contacts per person per day
     * @param infectivity        fraction of contacts that transmit infection
     * @param initialSusceptible initial susceptible population
     * @param initialInfectious  initial infectious population
     * @param initialRecovered   initial recovered population
     * @param recoveryProportion fraction of infectious that recover per day
     * @return a fully wired {@link Model} ready for simulation
     */
    public static Model build(String modelName, double contactRate, double infectivity,
                              double initialSusceptible, double initialInfectious,
                              double initialRecovered, double recoveryProportion) {
        Model model = new Model(modelName);
        model.setMetadata(ModelMetadata.builder()
                .source("Kermack & McKendrick SIR model (1927)")
                .license("CC-BY-SA-4.0")
                .build());

        Stock susceptible = new Stock("Susceptible", initialSusceptible, PEOPLE);
        Stock infectious = new Stock("Infectious", initialInfectious, PEOPLE);
        Stock recovered = new Stock("Recovered", initialRecovered, PEOPLE);

        Flow infectionRate = Flow.create("Infected", DAY, () -> {
            double newInfections = computeNewInfections(contactRate, infectivity,
                    susceptible.getValue(), infectious.getValue(), recovered.getValue());
            return new Quantity(newInfections, PEOPLE);
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

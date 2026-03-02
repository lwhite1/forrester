package com.deathrayresearch.forrester.model.def;

/**
 * Default simulation settings for a model definition.
 *
 * @param timeStep the time step unit name
 * @param duration the simulation duration amount
 * @param durationUnit the duration unit name
 */
public record SimulationSettings(
        String timeStep,
        double duration,
        String durationUnit
) {

    public SimulationSettings {
        if (timeStep == null || timeStep.isBlank()) {
            throw new IllegalArgumentException("Time step must not be blank");
        }
        if (duration <= 0) {
            throw new IllegalArgumentException("Duration must be positive");
        }
    }
}

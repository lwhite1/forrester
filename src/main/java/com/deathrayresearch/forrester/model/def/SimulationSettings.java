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
        if (Double.isNaN(duration) || Double.isInfinite(duration)) {
            throw new IllegalArgumentException("Duration must be finite, got " + duration);
        }
        if (duration <= 0) {
            throw new IllegalArgumentException("Duration must be positive");
        }
        if (durationUnit == null || durationUnit.isBlank()) {
            throw new IllegalArgumentException("Duration unit must not be blank");
        }
    }
}

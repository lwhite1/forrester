package systems.courant.shrewd.model.def;

/**
 * Default simulation settings for a model definition.
 *
 * @param timeStep     the time step unit name
 * @param duration     the simulation duration amount
 * @param durationUnit the duration unit name
 * @param dt           the fractional time step within the unit (e.g., 0.25 means each step
 *                     advances by 0.25 of the time step unit). Defaults to 1.0.
 */
public record SimulationSettings(
        String timeStep,
        double duration,
        String durationUnit,
        double dt
) {

    /**
     * Creates simulation settings with dt defaulting to 1.0.
     */
    public SimulationSettings(String timeStep, double duration, String durationUnit) {
        this(timeStep, duration, durationUnit, 1.0);
    }

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
        if (dt <= 0 || Double.isNaN(dt) || Double.isInfinite(dt)) {
            throw new IllegalArgumentException("dt must be positive and finite, got " + dt);
        }
    }
}

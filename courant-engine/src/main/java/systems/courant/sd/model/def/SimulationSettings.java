package systems.courant.sd.model.def;

/**
 * Default simulation settings for a model definition.
 *
 * @param timeStep     the time step unit name
 * @param duration     the simulation duration amount
 * @param durationUnit the duration unit name
 * @param dt           the fractional time step within the unit (e.g., 0.25 means each step
 *                     advances by 0.25 of the time step unit). Defaults to 1.0.
 * @param strictMode   when true, the simulation throws on non-finite values instead of
 *                     reverting to the previous value. Defaults to false.
 * @param savePer      the recording interval in steps. Only every Nth step is recorded
 *                     to history and fires time step events. Defaults to 1 (record all).
 */
public record SimulationSettings(
        String timeStep,
        double duration,
        String durationUnit,
        double dt,
        boolean strictMode,
        long savePer
) {

    /**
     * Creates simulation settings with dt defaulting to 1.0, strictMode off, and savePer 1.
     */
    public SimulationSettings(String timeStep, double duration, String durationUnit) {
        this(timeStep, duration, durationUnit, 1.0, false, 1);
    }

    /**
     * Creates simulation settings with strictMode off and savePer 1.
     */
    public SimulationSettings(String timeStep, double duration, String durationUnit, double dt) {
        this(timeStep, duration, durationUnit, dt, false, 1);
    }

    /**
     * Creates simulation settings with savePer defaulting to 1.
     */
    public SimulationSettings(String timeStep, double duration, String durationUnit, double dt,
                              boolean strictMode) {
        this(timeStep, duration, durationUnit, dt, strictMode, 1);
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
        if (savePer < 1) {
            throw new IllegalArgumentException("savePer must be >= 1, got " + savePer);
        }
    }
}

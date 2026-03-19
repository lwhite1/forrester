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
 * @param initialTime  the simulation start time as a numeric value (e.g., 0 or 1990).
 *                     Defaults to 0. Used to derive INITIAL_TIME and FINAL_TIME constants
 *                     for models that reference them in equations.
 */
public record SimulationSettings(
        String timeStep,
        double duration,
        String durationUnit,
        double dt,
        boolean strictMode,
        long savePer,
        double initialTime
) {

    /**
     * Creates simulation settings with dt defaulting to 1.0, strictMode off, savePer 1,
     * and initialTime 0.
     */
    public SimulationSettings(String timeStep, double duration, String durationUnit) {
        this(timeStep, duration, durationUnit, 1.0, false, 1, 0.0);
    }

    /**
     * Creates simulation settings with strictMode off, savePer 1, and initialTime 0.
     */
    public SimulationSettings(String timeStep, double duration, String durationUnit, double dt) {
        this(timeStep, duration, durationUnit, dt, false, 1, 0.0);
    }

    /**
     * Creates simulation settings with savePer defaulting to 1 and initialTime 0.
     */
    public SimulationSettings(String timeStep, double duration, String durationUnit, double dt,
                              boolean strictMode) {
        this(timeStep, duration, durationUnit, dt, strictMode, 1, 0.0);
    }

    /**
     * Creates simulation settings with initialTime defaulting to 0.
     */
    public SimulationSettings(String timeStep, double duration, String durationUnit, double dt,
                              boolean strictMode, long savePer) {
        this(timeStep, duration, durationUnit, dt, strictMode, savePer, 0.0);
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
        if (Double.isNaN(initialTime) || Double.isInfinite(initialTime)) {
            throw new IllegalArgumentException("initialTime must be finite, got " + initialTime);
        }
    }
}

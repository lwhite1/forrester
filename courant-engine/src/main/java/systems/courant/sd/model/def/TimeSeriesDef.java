package systems.courant.sd.model.def;

/**
 * Definition of an exogenous input time-series that drives a model variable
 * with external data rather than an equation.
 *
 * <p>At each simulation time step, the value is obtained by interpolating the
 * provided data points. This supports scenarios where a model is driven by
 * observed real-world data (e.g., historical order volumes, reported case counts).
 *
 * @param name           the variable name
 * @param timeValues     the time points at which data is available (must be sorted ascending)
 * @param dataValues     the data values at each time point (same length as timeValues)
 * @param unit           the unit of measure (may be null)
 * @param comment        optional description
 * @param interpolation  interpolation mode: "LINEAR" (default) or "STEP"
 * @param extrapolation  extrapolation mode: "HOLD" (default, holds last value) or "ZERO"
 */
public record TimeSeriesDef(
        String name,
        double[] timeValues,
        double[] dataValues,
        String unit,
        String comment,
        String interpolation,
        String extrapolation
) {

    public TimeSeriesDef {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Time series name must not be blank");
        }
        if (timeValues == null || dataValues == null) {
            throw new IllegalArgumentException("Time series data must not be null");
        }
        if (timeValues.length != dataValues.length) {
            throw new IllegalArgumentException(
                    "timeValues and dataValues must have the same length");
        }
        if (timeValues.length < 2) {
            throw new IllegalArgumentException("Time series must have at least 2 data points");
        }
        timeValues = timeValues.clone();
        dataValues = dataValues.clone();
        if (interpolation == null) {
            interpolation = "LINEAR";
        }
        if (extrapolation == null) {
            extrapolation = "HOLD";
        }
    }

    /**
     * Convenience constructor with default interpolation and extrapolation.
     */
    public TimeSeriesDef(String name, double[] timeValues, double[] dataValues, String unit) {
        this(name, timeValues, dataValues, unit, null, "LINEAR", "HOLD");
    }
}

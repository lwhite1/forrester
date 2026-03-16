package systems.courant.sd.model;

import java.util.Arrays;
import java.util.function.DoubleSupplier;

/**
 * A time-indexed data series that implements {@link Formula}, providing
 * exogenous input values interpolated from external data at each time step.
 *
 * <p>Unlike a {@link LookupTable} (which maps an arbitrary input expression
 * to an output value), a TimeSeries maps simulation time to a data value.
 * This is the mechanism for driving a model with observed real-world data.
 *
 * <p>Interpolation modes:
 * <ul>
 *     <li><b>LINEAR</b> — linear interpolation between data points</li>
 *     <li><b>STEP</b> — step function (holds each value until the next data point)</li>
 * </ul>
 *
 * <p>Extrapolation modes (for times outside the data range):
 * <ul>
 *     <li><b>HOLD</b> — holds the first/last data value (default)</li>
 *     <li><b>ZERO</b> — returns 0 outside the data range</li>
 * </ul>
 */
public class TimeSeries implements Formula {

    private final double[] timeValues;
    private final double[] dataValues;
    private final DoubleSupplier timeSupplier;
    private final boolean stepInterpolation;
    private final boolean zeroExtrapolation;

    private TimeSeries(double[] timeValues, double[] dataValues,
                       DoubleSupplier timeSupplier,
                       boolean stepInterpolation, boolean zeroExtrapolation) {
        this.timeValues = timeValues;
        this.dataValues = dataValues;
        this.timeSupplier = timeSupplier;
        this.stepInterpolation = stepInterpolation;
        this.zeroExtrapolation = zeroExtrapolation;
    }

    /**
     * Creates a time series with linear interpolation and hold extrapolation.
     */
    public static TimeSeries linear(double[] timeValues, double[] dataValues,
                                     DoubleSupplier timeSupplier) {
        return new TimeSeries(timeValues.clone(), dataValues.clone(),
                timeSupplier, false, false);
    }

    /**
     * Creates a time series with step interpolation and hold extrapolation.
     */
    public static TimeSeries step(double[] timeValues, double[] dataValues,
                                   DoubleSupplier timeSupplier) {
        return new TimeSeries(timeValues.clone(), dataValues.clone(),
                timeSupplier, true, false);
    }

    /**
     * Creates a time series with configurable interpolation and extrapolation.
     *
     * @param timeValues     sorted ascending time points
     * @param dataValues     data values at each time point
     * @param timeSupplier   supplies the current simulation time
     * @param interpolation  "LINEAR" or "STEP"
     * @param extrapolation  "HOLD" or "ZERO"
     */
    public static TimeSeries create(double[] timeValues, double[] dataValues,
                                     DoubleSupplier timeSupplier,
                                     String interpolation, String extrapolation) {
        boolean isStep = "STEP".equalsIgnoreCase(interpolation);
        boolean isZero = "ZERO".equalsIgnoreCase(extrapolation);
        return new TimeSeries(timeValues.clone(), dataValues.clone(),
                timeSupplier, isStep, isZero);
    }

    @Override
    public double getCurrentValue() {
        double t = timeSupplier.getAsDouble();

        // Before first data point
        if (t <= timeValues[0]) {
            return zeroExtrapolation ? 0.0 : dataValues[0];
        }

        // After last data point
        if (t >= timeValues[timeValues.length - 1]) {
            return zeroExtrapolation ? 0.0 : dataValues[dataValues.length - 1];
        }

        // Binary search for the interval containing t
        int idx = Arrays.binarySearch(timeValues, t);
        if (idx >= 0) {
            // Exact match
            return dataValues[idx];
        }

        // idx = -(insertion point) - 1, so insertion point = -(idx + 1)
        int insertionPoint = -(idx + 1);
        int lo = insertionPoint - 1;
        int hi = insertionPoint;

        if (stepInterpolation) {
            // Step function: hold the value at the lower bound
            return dataValues[lo];
        }

        // Linear interpolation
        double t0 = timeValues[lo];
        double t1 = timeValues[hi];
        double v0 = dataValues[lo];
        double v1 = dataValues[hi];
        double fraction = (t - t0) / (t1 - t0);
        return v0 + fraction * (v1 - v0);
    }
}

package systems.courant.shrewd.model;

import com.google.common.base.Preconditions;

import systems.courant.shrewd.model.compile.Resettable;

import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;

/**
 * Linear extrapolation forecast that implements {@link Formula}, providing the standard
 * SD FORECAST builtin.
 *
 * <p>FORECAST estimates where the input will be after {@code horizon} timesteps, based
 * on its current trend. It uses exponential smoothing to estimate the trend, then
 * extrapolates linearly:
 *
 * <pre>
 *     averageInput += (input - averageInput) / averagingTime
 *     trend = (input - averageInput) / (averageInput * averagingTime)
 *     forecast = input * (1 + trend * horizon)
 * </pre>
 *
 * <pre>{@code
 * Forecast fc = Forecast.of(() -> demand.getValue(), 10, 5, 0, sim::getCurrentStep);
 * }</pre>
 */
public class Forecast implements Formula, Resettable {

    private final DoubleSupplier input;
    private final double averagingTime;
    private final double horizon;
    private final double initialTrend;
    private final IntSupplier currentStep;

    private double averageInput;
    private double trend;
    private double lastInputVal;
    private boolean initialized;
    private int lastStep = -1;

    private Forecast(DoubleSupplier input, double averagingTime, double horizon,
                     double initialTrend, IntSupplier currentStep) {
        Preconditions.checkNotNull(input, "input supplier must not be null");
        Preconditions.checkNotNull(currentStep, "currentStep supplier must not be null");
        Preconditions.checkArgument(averagingTime > 0,
                "averagingTime must be positive, but got %s", averagingTime);
        this.input = input;
        this.averagingTime = averagingTime;
        this.horizon = horizon;
        this.initialTrend = initialTrend;
        this.currentStep = currentStep;
    }

    /**
     * Creates a FORECAST formula.
     *
     * @param input         supplies the current input value
     * @param averagingTime the smoothing time for trend estimation
     * @param horizon       number of timesteps to forecast ahead
     * @param initialTrend  the initial fractional growth rate
     * @param currentStep   supplies the current simulation timestep
     * @return a new Forecast formula
     */
    public static Forecast of(DoubleSupplier input, double averagingTime, double horizon,
                              double initialTrend, IntSupplier currentStep) {
        return new Forecast(input, averagingTime, horizon, initialTrend, currentStep);
    }

    /**
     * Resets this Forecast to its uninitialized state so it can be reused across simulation runs.
     */
    @Override
    public void reset() {
        averageInput = 0;
        trend = 0;
        lastInputVal = 0;
        initialized = false;
        lastStep = -1;
    }

    /**
     * Computes and returns the forecasted value for the current timestep.
     * Extrapolates linearly from the smoothed trend estimate.
     *
     * @return the forecasted value {@code input * (1 + trend * horizon)}
     */
    @Override
    public double getCurrentValue() {
        int step = currentStep.getAsInt();
        if (!initialized) {
            lastInputVal = input.getAsDouble();
            double denom = 1 + initialTrend * averagingTime;
            averageInput = denom != 0 ? lastInputVal / denom : lastInputVal;
            trend = initialTrend;
            initialized = true;
            lastStep = step;
        } else if (step > lastStep) {
            int delta = step - lastStep;
            for (int d = 0; d < delta; d++) {
                lastInputVal = input.getAsDouble();
                averageInput += (lastInputVal - averageInput) / averagingTime;
                if (averageInput != 0) {
                    trend = (lastInputVal - averageInput) / (averageInput * averagingTime);
                } else {
                    trend = 0;
                }
            }
            lastStep = step;
        }
        return lastInputVal * (1 + trend * horizon);
    }
}

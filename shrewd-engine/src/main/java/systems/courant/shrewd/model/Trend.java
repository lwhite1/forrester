package systems.courant.shrewd.model;

import com.google.common.base.Preconditions;

import systems.courant.shrewd.model.compile.Resettable;

import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;

/**
 * Computes the fractional rate of change of an input, providing the standard
 * SD TREND builtin.
 *
 * <p>TREND uses exponential smoothing to estimate the trend (fractional growth rate)
 * of the input signal. The equations (Euler integration, dt = 1 timestep) are:
 *
 * <pre>
 *     averageInput += (input - averageInput) / averagingTime
 *     trend = (input - averageInput) / (averageInput * averagingTime)
 * </pre>
 *
 * <p>The output is the fractional rate of change per timestep. For example, if the input
 * is growing at 5% per timestep, TREND returns approximately 0.05.
 *
 * <pre>{@code
 * Trend trend = Trend.of(() -> demand.getValue(), 10, 0, sim::getCurrentStep);
 * }</pre>
 */
public class Trend implements Formula, Resettable {

    private final DoubleSupplier input;
    private final double averagingTime;
    private final double initialTrend;
    private final IntSupplier currentStep;

    private double averageInput;
    private double trend;
    private boolean initialized;
    private int lastStep = -1;

    private Trend(DoubleSupplier input, double averagingTime, double initialTrend,
                  IntSupplier currentStep) {
        Preconditions.checkNotNull(input, "input supplier must not be null");
        Preconditions.checkNotNull(currentStep, "currentStep supplier must not be null");
        Preconditions.checkArgument(averagingTime > 0,
                "averagingTime must be positive, but got %s", averagingTime);
        this.input = input;
        this.averagingTime = averagingTime;
        this.initialTrend = initialTrend;
        this.currentStep = currentStep;
    }

    /**
     * Creates a TREND formula.
     *
     * @param input         supplies the current input value
     * @param averagingTime the smoothing time for trend estimation
     * @param initialTrend  the initial fractional growth rate
     * @param currentStep   supplies the current simulation timestep
     * @return a new Trend formula
     */
    public static Trend of(DoubleSupplier input, double averagingTime, double initialTrend,
                           IntSupplier currentStep) {
        return new Trend(input, averagingTime, initialTrend, currentStep);
    }

    /**
     * Resets this Trend to its uninitialized state so it can be reused across simulation runs.
     */
    @Override
    public void reset() {
        averageInput = 0;
        trend = 0;
        initialized = false;
        lastStep = -1;
    }

    /**
     * Returns the estimated fractional rate of change of the input for the current timestep.
     * On the first call, initializes the average so that the initial trend is correct.
     * On subsequent calls, updates the exponentially smoothed average and derives the trend.
     *
     * @return the fractional growth rate per timestep (e.g., 0.05 for 5% growth)
     */
    @Override
    public double getCurrentValue() {
        int step = currentStep.getAsInt();
        if (!initialized) {
            double inputVal = input.getAsDouble();
            // Initialize average so that the initial trend is correct:
            // trend = (input - avg) / (avg * avgTime)
            // => avg = input / (1 + initialTrend * avgTime)
            double denom = 1 + initialTrend * averagingTime;
            averageInput = denom != 0 ? inputVal / denom : inputVal;
            trend = initialTrend;
            initialized = true;
            lastStep = step;
        } else if (step > lastStep) {
            int delta = step - lastStep;
            for (int d = 0; d < delta; d++) {
                double inputVal = input.getAsDouble();
                averageInput += (inputVal - averageInput) / averagingTime;
                if (averageInput != 0) {
                    trend = (inputVal - averageInput) / (averageInput * averagingTime);
                } else {
                    trend = 0;
                }
            }
            lastStep = step;
        }
        return trend;
    }
}

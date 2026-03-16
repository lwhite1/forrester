package systems.courant.sd.model;

import com.google.common.base.Preconditions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import systems.courant.sd.model.compile.Resettable;

import java.util.function.DoubleSupplier;
import java.util.function.LongSupplier;

/**
 * First-order exponential smoothing that implements {@link Formula}, providing the standard
 * SD SMOOTH builtin (also known as a first-order information delay).
 *
 * <p>SMOOTH progressively adjusts toward its input over a specified smoothing time.
 * The underlying equation (Euler integration, dt = 1 timestep) is:
 *
 * <pre>
 *     smoothed += (input - smoothed) / smoothingTime
 * </pre>
 *
 * <p>where {@code smoothingTime} is expressed in simulation timesteps.
 *
 * <p>If no initial value is provided, the first input value is used (standard SD convention).
 *
 * <p>Response characteristics: after one smoothing time, the output reaches ~63% of a
 * step change in the input. After three smoothing times, it reaches ~95%. This makes
 * SMOOTH useful for modeling perception delays, trend averaging, and adaptive expectations.
 *
 * <pre>{@code
 * // Smooth perceived demand over 5 timesteps
 * Smooth perceived = Smooth.of(() -> actualDemand.getValue(), 5, sim::getCurrentStep);
 * Variable perceivedDemand = new Variable("Perceived Demand", THING, perceived);
 * }</pre>
 */
public class Smooth implements Formula, Resettable {

    private static final Logger log = LoggerFactory.getLogger(Smooth.class);

    private final DoubleSupplier input;
    private final DoubleSupplier smoothingTime;
    private final LongSupplier currentStep;
    private final double explicitInitial;
    private final boolean hasExplicitInitial;

    private double smoothed;
    private boolean initialized;
    private long lastStep = -1;
    private boolean warnedNonPositive;

    private Smooth(DoubleSupplier input, DoubleSupplier smoothingTime, LongSupplier currentStep,
                   double explicitInitial, boolean hasExplicitInitial) {
        Preconditions.checkNotNull(input, "input supplier must not be null");
        Preconditions.checkNotNull(smoothingTime, "smoothingTime supplier must not be null");
        Preconditions.checkNotNull(currentStep, "currentStep supplier must not be null");
        this.input = input;
        this.smoothingTime = smoothingTime;
        this.currentStep = currentStep;
        this.explicitInitial = explicitInitial;
        this.hasExplicitInitial = hasExplicitInitial;
    }

    /**
     * Creates a SMOOTH formula with the initial value set to the first input value.
     *
     * @param input         supplies the current input value to smooth
     * @param smoothingTime supplies the averaging time in simulation timesteps (re-evaluated each step)
     * @param currentStep   supplies the current simulation timestep
     * @return a new Smooth formula
     */
    public static Smooth of(DoubleSupplier input, DoubleSupplier smoothingTime,
                            LongSupplier currentStep) {
        return new Smooth(input, smoothingTime, currentStep, 0, false);
    }

    /**
     * Creates a SMOOTH formula with a constant smoothing time and the initial value
     * set to the first input value.
     *
     * @param input         supplies the current input value to smooth
     * @param smoothingTime the constant averaging time in simulation timesteps
     * @param currentStep   supplies the current simulation timestep
     * @return a new Smooth formula
     */
    public static Smooth of(DoubleSupplier input, double smoothingTime,
                            LongSupplier currentStep) {
        Preconditions.checkArgument(smoothingTime > 0,
                "smoothingTime must be positive, but got %s", smoothingTime);
        return new Smooth(input, () -> smoothingTime, currentStep, 0, false);
    }

    /**
     * Creates a SMOOTH formula with an explicit initial value.
     *
     * @param input         supplies the current input value to smooth
     * @param smoothingTime supplies the averaging time in simulation timesteps (re-evaluated each step)
     * @param initialValue  the smoothed value at time zero
     * @param currentStep   supplies the current simulation timestep
     * @return a new Smooth formula
     */
    public static Smooth of(DoubleSupplier input, DoubleSupplier smoothingTime,
                            double initialValue, LongSupplier currentStep) {
        return new Smooth(input, smoothingTime, currentStep, initialValue, true);
    }

    /**
     * Creates a SMOOTH formula with a constant smoothing time and explicit initial value.
     *
     * @param input         supplies the current input value to smooth
     * @param smoothingTime the constant averaging time in simulation timesteps
     * @param initialValue  the smoothed value at time zero
     * @param currentStep   supplies the current simulation timestep
     * @return a new Smooth formula
     */
    public static Smooth of(DoubleSupplier input, double smoothingTime, double initialValue,
                            LongSupplier currentStep) {
        Preconditions.checkArgument(smoothingTime > 0,
                "smoothingTime must be positive, but got %s", smoothingTime);
        return new Smooth(input, () -> smoothingTime, currentStep, initialValue, true);
    }

    /**
     * Resets this Smooth to its uninitialized state so it can be reused across simulation runs.
     * The next call to {@link #getCurrentValue()} will re-initialize from the input or explicit initial value.
     */
    @Override
    public void reset() {
        smoothed = 0;
        initialized = false;
        lastStep = -1;
        warnedNonPositive = false;
    }

    /**
     * Returns the exponentially smoothed value for the current timestep.
     * On the first call, initializes from the input or explicit initial value.
     * On subsequent calls, adjusts toward the input by {@code (input - smoothed) / smoothingTime}
     * for each elapsed timestep.
     *
     * @return the smoothed value
     */
    @Override
    public double getCurrentValue() {
        long step = currentStep.getAsLong();
        if (!initialized) {
            smoothed = hasExplicitInitial ? explicitInitial : input.getAsDouble();
            initialized = true;
            lastStep = step;
        } else if (step > lastStep) {
            long delta = step - lastStep;
            double inputVal = input.getAsDouble();
            double st = smoothingTime.getAsDouble();
            if (st <= 0) {
                if (!warnedNonPositive) {
                    log.warn("SMOOTH: smoothingTime is {} (non-positive), clamping to 1.0", st);
                    warnedNonPositive = true;
                }
                st = 1.0;
            }
            for (int i = 0; i < delta; i++) {
                smoothed += (inputVal - smoothed) / st;
            }
            lastStep = step;
        }
        return smoothed;
    }
}

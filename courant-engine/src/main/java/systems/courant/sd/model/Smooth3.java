package systems.courant.sd.model;

import com.google.common.base.Preconditions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import systems.courant.sd.model.compile.Resettable;

import java.util.function.DoubleSupplier;
import java.util.function.LongSupplier;

/**
 * Third-order exponential smoothing that implements {@link Formula}, providing the standard
 * SD SMOOTH3 builtin (also known as a third-order information delay).
 *
 * <p>SMOOTH3 chains three first-order exponential smooths, each with a smoothing time of
 * {@code smoothingTime / 3}. This produces a more delayed, S-shaped response compared
 * to first-order {@link Smooth}. The underlying equations (Euler integration, dt = 1) are:
 *
 * <pre>
 *     stageTime = smoothingTime / 3
 *     stage1 += (input  - stage1) / stageTime
 *     stage2 += (stage1 - stage2) / stageTime
 *     stage3 += (stage2 - stage3) / stageTime
 *     output = stage3
 * </pre>
 *
 * <p>If no initial value is provided, the first input value is used (standard SD convention).
 *
 * <p>Response characteristics: compared to first-order SMOOTH, the output has a longer
 * initial lag and a steeper S-shaped transition. After one smoothing time, the output
 * reaches approximately 80% of a step change (vs ~63% for first-order).
 *
 * <pre>{@code
 * // Third-order smooth of perceived demand over 6 timesteps
 * Smooth3 perceived = Smooth3.of(() -> actualDemand.getValue(), 6, sim::getCurrentStep);
 * Variable perceivedDemand = new Variable("Perceived Demand", THING, perceived);
 * }</pre>
 */
public class Smooth3 implements Formula, Resettable {

    private static final Logger log = LoggerFactory.getLogger(Smooth3.class);

    private final DoubleSupplier input;
    private final DoubleSupplier smoothingTime;
    private final LongSupplier currentStep;
    private final double explicitInitial;
    private final boolean hasExplicitInitial;

    private double stage1;
    private double stage2;
    private double stage3;
    private double lastInputVal;
    private boolean initialized;
    private long lastStep = -1;
    private boolean warnedNonPositive;

    private Smooth3(DoubleSupplier input, DoubleSupplier smoothingTime, LongSupplier currentStep,
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
     * Creates a SMOOTH3 formula with the initial value set to the first input value.
     *
     * @param input         supplies the current input value to smooth
     * @param smoothingTime the averaging time in simulation timesteps
     * @param currentStep   supplies the current simulation timestep
     * @return a new Smooth3 formula
     */
    public static Smooth3 of(DoubleSupplier input, DoubleSupplier smoothingTime,
                             LongSupplier currentStep) {
        return new Smooth3(input, smoothingTime, currentStep, 0, false);
    }

    /**
     * Creates a SMOOTH3 formula with a constant smoothing time.
     */
    public static Smooth3 of(DoubleSupplier input, double smoothingTime,
                             LongSupplier currentStep) {
        Preconditions.checkArgument(smoothingTime > 0,
                "smoothingTime must be positive, but got %s", smoothingTime);
        return new Smooth3(input, () -> smoothingTime, currentStep, 0, false);
    }

    /**
     * Creates a SMOOTH3 formula with an explicit initial value.
     *
     * @param input         supplies the current input value to smooth
     * @param smoothingTime supplies the averaging time in simulation timesteps
     * @param initialValue  the smoothed value at time zero
     * @param currentStep   supplies the current simulation timestep
     * @return a new Smooth3 formula
     */
    public static Smooth3 of(DoubleSupplier input, DoubleSupplier smoothingTime,
                             double initialValue, LongSupplier currentStep) {
        return new Smooth3(input, smoothingTime, currentStep, initialValue, true);
    }

    /**
     * Creates a SMOOTH3 formula with a constant smoothing time and explicit initial value.
     */
    public static Smooth3 of(DoubleSupplier input, double smoothingTime, double initialValue,
                             LongSupplier currentStep) {
        Preconditions.checkArgument(smoothingTime > 0,
                "smoothingTime must be positive, but got %s", smoothingTime);
        return new Smooth3(input, () -> smoothingTime, currentStep, initialValue, true);
    }

    /**
     * Resets this Smooth3 to its uninitialized state so it can be reused across simulation runs.
     */
    @Override
    public void reset() {
        stage1 = 0;
        stage2 = 0;
        stage3 = 0;
        lastInputVal = 0;
        initialized = false;
        lastStep = -1;
        warnedNonPositive = false;
    }

    /**
     * Returns the third-order smoothed value for the current timestep.
     * On the first call, initializes all three stages from the input or explicit initial value.
     * On subsequent calls, cascades the smoothing through three stages for each elapsed timestep.
     *
     * @return the smoothed value (output of the third stage)
     */
    @Override
    public double getCurrentValue() {
        long step = currentStep.getAsLong();
        if (!initialized) {
            double inputAtInit = input.getAsDouble();
            double init = hasExplicitInitial ? explicitInitial : inputAtInit;
            stage1 = init;
            stage2 = init;
            stage3 = init;
            lastInputVal = inputAtInit;
            initialized = true;
            lastStep = step;
        } else if (step > lastStep) {
            double st = smoothingTime.getAsDouble();
            if (st <= 0) {
                if (!warnedNonPositive) {
                    log.warn("SMOOTH3: smoothingTime is {} (non-positive), clamping to 3.0", st);
                    warnedNonPositive = true;
                }
                st = 3.0;
            }
            double stageTime = st / 3.0;
            long delta = step - lastStep;
            double currentInput = input.getAsDouble();
            for (long i = 0; i < delta; i++) {
                double inputVal = (i < delta - 1) ? lastInputVal : currentInput;
                stage1 += (inputVal - stage1) / stageTime;
                stage2 += (stage1 - stage2) / stageTime;
                stage3 += (stage2 - stage3) / stageTime;
            }
            lastInputVal = currentInput;
            lastStep = step;
        }
        return stage3;
    }
}

package systems.courant.sd.model;

import com.google.common.base.Preconditions;

import systems.courant.sd.model.compile.Resettable;

import java.util.function.DoubleSupplier;
import java.util.function.LongSupplier;

/**
 * A first-order material delay that implements {@link Formula}, providing the standard
 * SD DELAY1 builtin.
 *
 * <p>DELAY1 uses a single material delay stage to produce an exponentially delayed
 * version of the input signal. Unlike {@link Delay3} (which chains three stages for an
 * S-shaped response), DELAY1 produces an immediate partial response that decays
 * exponentially — there is no initial lag.
 *
 * <p>Like {@link Delay3}, DELAY1 conserves material — the quantity in transit equals the
 * stage level. The Euler integration equations (dt = 1 timestep) are:
 *
 * <pre>
 *     rate = stage / delayTime
 *     stage += input - rate
 *     output = rate
 * </pre>
 *
 * <p>where {@code delayTime} is the delay expressed in simulation timesteps.
 *
 * <p>If no initial value is provided, the first input value is used (standard SD convention).
 * The stage is initialized so that the output equals the initial value at time zero.
 *
 * <p>Response characteristics: a step increase in input produces an immediate jump in output
 * that then exponentially approaches the new input level, reaching ~63% after one delay time
 * and ~95% after three delay times.
 *
 * <pre>{@code
 * // Delay material shipments by 6 timesteps (first-order)
 * Delay1 delayed = Delay1.of(() -> orders.getValue(), 6, sim::getCurrentStep);
 * Variable shipments = new Variable("Shipments", THING, delayed);
 * }</pre>
 */
public class Delay1 implements Formula, Resettable {

    private final DoubleSupplier input;
    private final double delayTime;
    private final LongSupplier currentStep;
    private final double explicitInitial;
    private final boolean hasExplicitInitial;

    private double stage;
    private double output;
    private boolean initialized;
    private long lastStep = -1;

    private Delay1(DoubleSupplier input, double delayTime, LongSupplier currentStep,
                   double explicitInitial, boolean hasExplicitInitial) {
        Preconditions.checkNotNull(input, "input supplier must not be null");
        Preconditions.checkNotNull(currentStep, "currentStep supplier must not be null");
        Preconditions.checkArgument(delayTime > 0,
                "delayTime must be positive, but got %s", delayTime);
        this.input = input;
        this.delayTime = delayTime;
        this.currentStep = currentStep;
        this.explicitInitial = explicitInitial;
        this.hasExplicitInitial = hasExplicitInitial;
    }

    /**
     * Creates a DELAY1 formula with the initial output set to the first input value.
     *
     * @param input       supplies the current input rate
     * @param delayTime   the delay in simulation timesteps
     * @param currentStep supplies the current simulation timestep
     * @return a new Delay1 formula
     */
    public static Delay1 of(DoubleSupplier input, double delayTime, LongSupplier currentStep) {
        return new Delay1(input, delayTime, currentStep, 0, false);
    }

    /**
     * Creates a DELAY1 formula with an explicit initial output value (DELAY1I semantics).
     *
     * @param input        supplies the current input rate
     * @param delayTime    the delay in simulation timesteps
     * @param initialValue the output rate at time zero
     * @param currentStep  supplies the current simulation timestep
     * @return a new Delay1 formula
     */
    public static Delay1 of(DoubleSupplier input, double delayTime, double initialValue,
                            LongSupplier currentStep) {
        return new Delay1(input, delayTime, currentStep, initialValue, true);
    }

    /**
     * Resets this Delay1 to its uninitialized state so it can be reused across simulation runs.
     * The next call to {@link #getCurrentValue()} will re-initialize from the input or explicit initial value.
     */
    @Override
    public void reset() {
        stage = 0;
        output = 0;
        initialized = false;
        lastStep = -1;
    }

    /**
     * Computes and returns the delayed output value for the current timestep.
     * On the first call, initializes the delay stage. On subsequent calls,
     * advances the stage using Euler integration for each elapsed timestep.
     *
     * @return the output rate from the delay stage
     */
    @Override
    public double getCurrentValue() {
        long step = currentStep.getAsLong();
        if (!initialized) {
            double init = hasExplicitInitial ? explicitInitial : input.getAsDouble();
            stage = init * delayTime;
            output = init;
            initialized = true;
            lastStep = step;
        } else if (step > lastStep) {
            long delta = step - lastStep;
            for (int d = 0; d < delta; d++) {
                double inputVal = input.getAsDouble();
                double rate = stage / delayTime;
                stage += inputVal - rate;
                output = rate;
            }
            lastStep = step;
        }
        return output;
    }
}

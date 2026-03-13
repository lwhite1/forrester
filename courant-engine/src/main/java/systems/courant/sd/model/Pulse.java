package systems.courant.sd.model;

import com.google.common.base.Preconditions;

import java.util.function.IntSupplier;

/**
 * A pulse function that implements {@link Formula}, providing the standard SD PULSE builtin.
 *
 * <p>Returns a constant magnitude for one timestep at the start time, then zero.
 * If an interval is specified, the pulse repeats every {@code interval} timesteps.
 *
 * <p>Times are expressed in simulation timesteps (e.g., if the simulation runs
 * with a DAY timestep and startStep is 10, the first pulse occurs on day 10).
 *
 * <pre>{@code
 * // Single pulse of 100 at step 10
 * Pulse pulse = Pulse.of(100, 10, sim::getCurrentStep);
 *
 * // Repeating pulse of 100 at step 10, then every 5 steps
 * Pulse pulse = Pulse.of(100, 10, 5, sim::getCurrentStep);
 * }</pre>
 */
public class Pulse implements Formula {

    private final double magnitude;
    private final int startStep;
    private final int interval;
    private final IntSupplier currentStep;

    private Pulse(double magnitude, int startStep, int interval, IntSupplier currentStep) {
        Preconditions.checkNotNull(currentStep, "currentStep supplier must not be null");
        Preconditions.checkArgument(startStep >= 0,
                "startStep must be non-negative, but got %s", startStep);
        Preconditions.checkArgument(interval >= 0,
                "interval must be non-negative, but got %s", interval);
        this.magnitude = magnitude;
        this.startStep = startStep;
        this.interval = interval;
        this.currentStep = currentStep;
    }

    /**
     * Creates a single (non-repeating) pulse.
     *
     * @param magnitude   the value returned during the pulse
     * @param startStep   the timestep at which the pulse fires
     * @param currentStep supplies the current simulation timestep
     * @return a new Pulse formula
     */
    public static Pulse of(double magnitude, int startStep, IntSupplier currentStep) {
        return new Pulse(magnitude, startStep, 0, currentStep);
    }

    /**
     * Creates a repeating pulse.
     *
     * @param magnitude   the value returned during each pulse
     * @param startStep   the timestep at which the first pulse fires
     * @param interval    the number of timesteps between pulses (0 = no repeat)
     * @param currentStep supplies the current simulation timestep
     * @return a new Pulse formula
     */
    public static Pulse of(double magnitude, int startStep, int interval, IntSupplier currentStep) {
        return new Pulse(magnitude, startStep, interval, currentStep);
    }

    /**
     * Returns the pulse magnitude if the current timestep is a pulse timestep, or zero otherwise.
     * For repeating pulses, fires at the start step and every {@code interval} steps thereafter.
     *
     * @return the magnitude during a pulse, or zero
     */
    @Override
    public double getCurrentValue() {
        int step = currentStep.getAsInt();
        if (step < startStep) {
            return 0;
        }
        if (step == startStep) {
            return magnitude;
        }
        if (interval > 0 && (step - startStep) % interval == 0) {
            return magnitude;
        }
        return 0;
    }
}

package systems.courant.shrewd.model;

import com.google.common.base.Preconditions;

import systems.courant.shrewd.model.compile.Resettable;

import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;

/**
 * A fixed pipeline delay that implements {@link Formula}, providing the standard
 * SD DELAY_FIXED builtin.
 *
 * <p>DELAY_FIXED returns the input value from exactly {@code delayTime} timesteps ago.
 * Unlike {@link Delay3} (which smooths the output through three stages), DELAY_FIXED
 * produces a pure time-shifted copy of the input — a step change in input appears as
 * a step change in output after the delay.
 *
 * <p>Internally maintains a circular buffer of past input values.
 *
 * <pre>{@code
 * // Output equals input from exactly 5 timesteps ago
 * DelayFixed delayed = DelayFixed.of(() -> orders.getValue(), 5, 0, sim::getCurrentStep);
 * }</pre>
 */
public class DelayFixed implements Formula, Resettable {

    private final DoubleSupplier input;
    private final int delaySteps;
    private final DoubleSupplier initialValueSupplier;
    private final IntSupplier currentStep;

    private double[] buffer;
    private int writeIndex;
    private boolean initialized;
    private int lastStep = -1;

    private DelayFixed(DoubleSupplier input, int delaySteps, DoubleSupplier initialValueSupplier,
                       IntSupplier currentStep) {
        Preconditions.checkNotNull(input, "input supplier must not be null");
        Preconditions.checkNotNull(initialValueSupplier,
                "initialValue supplier must not be null");
        Preconditions.checkNotNull(currentStep, "currentStep supplier must not be null");
        Preconditions.checkArgument(delaySteps > 0,
                "delaySteps must be positive, but got %s", delaySteps);
        this.input = input;
        this.delaySteps = delaySteps;
        this.initialValueSupplier = initialValueSupplier;
        this.currentStep = currentStep;
    }

    /**
     * Creates a DELAY_FIXED formula.
     *
     * @param input        supplies the current input value
     * @param delaySteps   the fixed delay in simulation timesteps
     * @param initialValue the output value before the delay has elapsed
     * @param currentStep  supplies the current simulation timestep
     * @return a new DelayFixed formula
     */
    public static DelayFixed of(DoubleSupplier input, int delaySteps, double initialValue,
                                IntSupplier currentStep) {
        return new DelayFixed(input, delaySteps, () -> initialValue, currentStep);
    }

    /**
     * Creates a DELAY_FIXED formula with a dynamic initial value.
     * The initial value expression is evaluated once when the delay is first used.
     *
     * @param input                supplies the current input value
     * @param delaySteps           the fixed delay in simulation timesteps
     * @param initialValueSupplier supplies the initial value (evaluated once at step 0)
     * @param currentStep          supplies the current simulation timestep
     * @return a new DelayFixed formula
     */
    public static DelayFixed of(DoubleSupplier input, int delaySteps,
                                DoubleSupplier initialValueSupplier,
                                IntSupplier currentStep) {
        return new DelayFixed(input, delaySteps, initialValueSupplier, currentStep);
    }

    /**
     * Resets this DelayFixed to its uninitialized state so it can be reused across simulation runs.
     */
    @Override
    public void reset() {
        buffer = null;
        writeIndex = 0;
        initialized = false;
        lastStep = -1;
    }

    /**
     * Returns the input value from exactly {@code delaySteps} timesteps ago.
     * Before the delay has elapsed, returns the initial value.
     *
     * @return the delayed value for the current timestep
     */
    @Override
    public double getCurrentValue() {
        int step = currentStep.getAsInt();
        if (!initialized) {
            buffer = new double[delaySteps + 1];
            java.util.Arrays.fill(buffer, initialValueSupplier.getAsDouble());
            writeIndex = 0;
            initialized = true;
            lastStep = step - 1;
        }
        if (step > lastStep) {
            double currentInput = input.getAsDouble();
            int delta = step - lastStep;
            // For missed intermediate steps, repeat the last written input (zero-order hold)
            double lastKnownInput = buffer[(writeIndex - 1 + buffer.length) % buffer.length];
            for (int d = 0; d < delta - 1; d++) {
                buffer[writeIndex] = lastKnownInput;
                writeIndex = (writeIndex + 1) % buffer.length;
            }
            // Write the current input only for the actual current step
            buffer[writeIndex] = currentInput;
            writeIndex = (writeIndex + 1) % buffer.length;
            lastStep = step;
        }
        // After write-and-advance, writeIndex points to the oldest value in the buffer
        return buffer[writeIndex];
    }
}

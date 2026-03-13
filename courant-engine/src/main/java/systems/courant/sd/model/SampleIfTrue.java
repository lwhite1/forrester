package systems.courant.sd.model;

import com.google.common.base.Preconditions;

import systems.courant.sd.model.compile.Resettable;

import java.util.function.DoubleSupplier;
import java.util.function.LongSupplier;

/**
 * Implements the Vensim SAMPLE IF TRUE function.
 *
 * <p>{@code SAMPLE IF TRUE(condition, input, initial)} samples {@code input} whenever
 * {@code condition} is non-zero (true), and holds the last sampled value when condition
 * is false. The initial value before the first true condition is {@code initial}.
 *
 * <p>This is a zero-order hold triggered by a boolean condition.
 */
public class SampleIfTrue implements Formula, Resettable {

    private final DoubleSupplier condition;
    private final DoubleSupplier input;
    private final double initialValue;
    private final LongSupplier currentStep;

    private double heldValue;
    private boolean initialized;
    private long lastStep = -1;

    private SampleIfTrue(DoubleSupplier condition, DoubleSupplier input,
                          double initialValue, LongSupplier currentStep) {
        Preconditions.checkNotNull(condition, "condition supplier must not be null");
        Preconditions.checkNotNull(input, "input supplier must not be null");
        Preconditions.checkNotNull(currentStep, "currentStep supplier must not be null");
        this.condition = condition;
        this.input = input;
        this.initialValue = initialValue;
        this.currentStep = currentStep;
    }

    /**
     * Creates a SAMPLE IF TRUE formula.
     *
     * @param condition    supplies the boolean condition (non-zero = true)
     * @param input        supplies the value to sample when condition is true
     * @param initialValue the value to return before the first true condition
     * @param currentStep  supplies the current simulation timestep
     * @return a new SampleIfTrue formula
     */
    public static SampleIfTrue of(DoubleSupplier condition, DoubleSupplier input,
                                   double initialValue, LongSupplier currentStep) {
        return new SampleIfTrue(condition, input, initialValue, currentStep);
    }

    @Override
    public void reset() {
        heldValue = 0;
        initialized = false;
        lastStep = -1;
    }

    @Override
    public double getCurrentValue() {
        long step = currentStep.getAsLong();
        if (!initialized) {
            initialized = true;
            lastStep = step;
            if (condition.getAsDouble() != 0.0) {
                heldValue = input.getAsDouble();
            } else {
                heldValue = initialValue;
            }
        } else if (step > lastStep) {
            if (condition.getAsDouble() != 0.0) {
                heldValue = input.getAsDouble();
            }
            lastStep = step;
        }
        return heldValue;
    }
}

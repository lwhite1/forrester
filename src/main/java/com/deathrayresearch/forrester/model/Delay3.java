package com.deathrayresearch.forrester.model;

import com.google.common.base.Preconditions;

import com.deathrayresearch.forrester.model.compile.Resettable;

import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;

/**
 * A third-order material delay that implements {@link Formula}, providing the standard
 * SD DELAY3 builtin.
 *
 * <p>DELAY3 chains three first-order material delay stages to produce a smoothed, delayed
 * version of the input signal. Unlike {@link Smooth} (an information delay), DELAY3
 * conserves material — the total quantity in transit equals the sum of the three stages.
 *
 * <p>Each stage has a residence time of {@code delayTime / 3}, and the output is the
 * outflow rate from the third stage. The Euler integration equations (dt = 1 timestep) are:
 *
 * <pre>
 *     stageTime = delayTime / 3
 *     rate1 = stage1 / stageTime     stage1 += input - rate1
 *     rate2 = stage2 / stageTime     stage2 += rate1 - rate2
 *     rate3 = stage3 / stageTime     stage3 += rate2 - rate3
 *     output = rate3
 * </pre>
 *
 * <p>where {@code delayTime} is the total delay expressed in simulation timesteps.
 *
 * <p>If no initial value is provided, the first input value is used (standard SD convention).
 * The three stages are initialized so that the output equals the initial value at time zero.
 *
 * <p>Response characteristics: a step increase in input produces a smooth S-shaped rise
 * in output, reaching ~95% of the new level after approximately 2x the delay time. The
 * peak output rate for a pulse input occurs at the delay time. This makes DELAY3 useful
 * for modeling supply chains, manufacturing pipelines, and construction delays.
 *
 * <pre>{@code
 * // Delay material shipments by 6 timesteps
 * Delay3 delayed = Delay3.of(() -> orders.getValue(), 6, sim::getCurrentStep);
 * Variable shipments = new Variable("Shipments", THING, delayed);
 * }</pre>
 */
public class Delay3 implements Formula, Resettable {

    private final DoubleSupplier input;
    private final double delayTime;
    private final IntSupplier currentStep;
    private final double explicitInitial;
    private final boolean hasExplicitInitial;

    private double stage1;
    private double stage2;
    private double stage3;
    private double output;
    private boolean initialized;
    private int lastStep = -1;

    private Delay3(DoubleSupplier input, double delayTime, IntSupplier currentStep,
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
     * Creates a DELAY3 formula with the initial output set to the first input value.
     *
     * @param input       supplies the current input rate
     * @param delayTime   the total delay in simulation timesteps
     * @param currentStep supplies the current simulation timestep
     * @return a new Delay3 formula
     */
    public static Delay3 of(DoubleSupplier input, double delayTime, IntSupplier currentStep) {
        return new Delay3(input, delayTime, currentStep, 0, false);
    }

    /**
     * Creates a DELAY3 formula with an explicit initial output value.
     *
     * @param input        supplies the current input rate
     * @param delayTime    the total delay in simulation timesteps
     * @param initialValue the output rate at time zero
     * @param currentStep  supplies the current simulation timestep
     * @return a new Delay3 formula
     */
    public static Delay3 of(DoubleSupplier input, double delayTime, double initialValue,
                            IntSupplier currentStep) {
        return new Delay3(input, delayTime, currentStep, initialValue, true);
    }

    /**
     * Resets this Delay3 to its uninitialized state so it can be reused across simulation runs.
     * The next call to {@link #getCurrentValue()} will re-initialize from the input or explicit initial value.
     */
    public void reset() {
        stage1 = 0;
        stage2 = 0;
        stage3 = 0;
        output = 0;
        initialized = false;
        lastStep = -1;
    }

    @Override
    public double getCurrentValue() {
        int step = currentStep.getAsInt();
        if (!initialized) {
            double init = hasExplicitInitial ? explicitInitial : input.getAsDouble();
            double stageTime = delayTime / 3.0;
            stage1 = init * stageTime;
            stage2 = init * stageTime;
            stage3 = init * stageTime;
            output = init;
            initialized = true;
            lastStep = step;
        } else if (step > lastStep) {
            double stageTime = delayTime / 3.0;
            int delta = step - lastStep;
            for (int d = 0; d < delta; d++) {
                // Compute outflow rates from current stage levels
                double rate1 = stage1 / stageTime;
                double rate2 = stage2 / stageTime;
                double rate3 = stage3 / stageTime;

                // Update stage levels (Euler integration, dt = 1 timestep)
                stage1 += input.getAsDouble() - rate1;
                stage2 += rate1 - rate2;
                stage3 += rate2 - rate3;

                output = rate3;
            }
            lastStep = step;
        }
        return output;
    }
}

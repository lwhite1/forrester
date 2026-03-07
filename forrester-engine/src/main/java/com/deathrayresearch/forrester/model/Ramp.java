package com.deathrayresearch.forrester.model;

import com.google.common.base.Preconditions;

import java.util.function.IntSupplier;

/**
 * A ramp function that implements {@link Formula}, providing the standard SD RAMP builtin.
 *
 * <p>Returns 0 before the start time, then increases linearly at the given slope.
 * If an end time is specified, the value holds constant after the end time.
 *
 * <p>Times are expressed in simulation timesteps (e.g., if the simulation runs
 * with a DAY timestep and startStep is 5, the ramp begins on day 5).
 *
 * <pre>{@code
 * // Unbounded ramp: increases by 10 per step starting at step 5
 * Ramp ramp = Ramp.of(10, 5, sim::getCurrentStep);
 *
 * // Bounded ramp: increases by 10 per step from step 5 to step 15, then holds
 * Ramp ramp = Ramp.of(10, 5, 15, sim::getCurrentStep);
 * }</pre>
 */
public class Ramp implements Formula {

    private final double slope;
    private final int startStep;
    private final int endStep;
    private final IntSupplier currentStep;

    private Ramp(double slope, int startStep, int endStep, IntSupplier currentStep) {
        Preconditions.checkNotNull(currentStep, "currentStep supplier must not be null");
        Preconditions.checkArgument(startStep >= 0,
                "startStep must be non-negative, but got %s", startStep);
        Preconditions.checkArgument(endStep >= startStep,
                "endStep (%s) must be >= startStep (%s)", endStep, startStep);
        this.slope = slope;
        this.startStep = startStep;
        this.endStep = endStep;
        this.currentStep = currentStep;
    }

    /**
     * Creates an unbounded ramp that increases indefinitely from the start time.
     *
     * @param slope       the rate of increase per timestep
     * @param startStep   the timestep at which the ramp begins
     * @param currentStep supplies the current simulation timestep
     * @return a new Ramp formula
     */
    public static Ramp of(double slope, int startStep, IntSupplier currentStep) {
        return new Ramp(slope, startStep, Integer.MAX_VALUE, currentStep);
    }

    /**
     * Creates a bounded ramp that increases from start to end time, then holds constant.
     *
     * @param slope       the rate of increase per timestep
     * @param startStep   the timestep at which the ramp begins
     * @param endStep     the timestep at which the ramp stops increasing
     * @param currentStep supplies the current simulation timestep
     * @return a new Ramp formula
     */
    public static Ramp of(double slope, int startStep, int endStep, IntSupplier currentStep) {
        return new Ramp(slope, startStep, endStep, currentStep);
    }

    /**
     * Returns zero before the start step, then increases linearly at the configured slope.
     * For bounded ramps, the value holds constant after the end step.
     *
     * @return the ramp value for the current timestep
     */
    @Override
    public double getCurrentValue() {
        int step = currentStep.getAsInt();
        if (step < startStep) {
            return 0;
        }
        int elapsed = Math.min(step, endStep) - startStep;
        return slope * elapsed;
    }
}

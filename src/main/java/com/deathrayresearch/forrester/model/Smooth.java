package com.deathrayresearch.forrester.model;

import com.google.common.base.Preconditions;

import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;

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
 * <pre>{@code
 * // Smooth perceived demand over 5 timesteps
 * Smooth perceived = Smooth.of(() -> actualDemand.getValue(), 5, sim::getCurrentStep);
 * Variable perceivedDemand = new Variable("Perceived Demand", THING, perceived);
 * }</pre>
 */
public class Smooth implements Formula {

    private final DoubleSupplier input;
    private final double smoothingTime;
    private final IntSupplier currentStep;
    private final double explicitInitial;
    private final boolean hasExplicitInitial;

    private double smoothed;
    private boolean initialized;
    private int lastStep = -1;

    private Smooth(DoubleSupplier input, double smoothingTime, IntSupplier currentStep,
                   double explicitInitial, boolean hasExplicitInitial) {
        Preconditions.checkNotNull(input, "input supplier must not be null");
        Preconditions.checkNotNull(currentStep, "currentStep supplier must not be null");
        Preconditions.checkArgument(smoothingTime > 0,
                "smoothingTime must be positive, but got %s", smoothingTime);
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
     * @param smoothingTime the averaging time in simulation timesteps
     * @param currentStep   supplies the current simulation timestep
     * @return a new Smooth formula
     */
    public static Smooth of(DoubleSupplier input, double smoothingTime, IntSupplier currentStep) {
        return new Smooth(input, smoothingTime, currentStep, 0, false);
    }

    /**
     * Creates a SMOOTH formula with an explicit initial value.
     *
     * @param input         supplies the current input value to smooth
     * @param smoothingTime the averaging time in simulation timesteps
     * @param initialValue  the smoothed value at time zero
     * @param currentStep   supplies the current simulation timestep
     * @return a new Smooth formula
     */
    public static Smooth of(DoubleSupplier input, double smoothingTime, double initialValue,
                            IntSupplier currentStep) {
        return new Smooth(input, smoothingTime, currentStep, initialValue, true);
    }

    @Override
    public double getCurrentValue() {
        int step = currentStep.getAsInt();
        if (!initialized) {
            smoothed = hasExplicitInitial ? explicitInitial : input.getAsDouble();
            initialized = true;
            lastStep = step;
        } else if (step > lastStep) {
            smoothed += (input.getAsDouble() - smoothed) / smoothingTime;
            lastStep = step;
        }
        return smoothed;
    }
}

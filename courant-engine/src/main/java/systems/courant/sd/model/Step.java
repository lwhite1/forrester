package systems.courant.sd.model;

import com.google.common.base.Preconditions;

import java.util.function.IntSupplier;

/**
 * A step function that implements {@link Formula}, providing the standard SD STEP builtin.
 *
 * <p>Returns 0 before the step time, and a constant height at and after the step time.
 * This is the fundamental input function for testing system responses to sudden changes.
 *
 * <p>The step time is expressed in simulation timesteps (e.g., if the simulation runs
 * with a DAY timestep and stepTime is 10, the step occurs on day 10).
 *
 * <pre>{@code
 * Step demandShock = Step.of(100, 10, sim::getCurrentStep);
 * Variable demand = new Variable("Demand Shock", THING, demandShock);
 * }</pre>
 */
public class Step implements Formula {

    private final double height;
    private final int stepTime;
    private final IntSupplier currentStep;

    private Step(double height, int stepTime, IntSupplier currentStep) {
        Preconditions.checkNotNull(currentStep, "currentStep supplier must not be null");
        Preconditions.checkArgument(stepTime >= 0,
                "stepTime must be non-negative, but got %s", stepTime);
        this.height = height;
        this.stepTime = stepTime;
        this.currentStep = currentStep;
    }

    /**
     * Creates a step function that returns 0 before {@code stepTime} and {@code height} afterwards.
     *
     * @param height      the value returned at and after the step time
     * @param stepTime    the timestep at which the step occurs
     * @param currentStep supplies the current simulation timestep
     * @return a new Step formula
     */
    public static Step of(double height, int stepTime, IntSupplier currentStep) {
        return new Step(height, stepTime, currentStep);
    }

    /**
     * Returns zero before the step time, and the configured height at and after the step time.
     *
     * @return the step value for the current timestep
     */
    @Override
    public double getCurrentValue() {
        return currentStep.getAsInt() >= stepTime ? height : 0;
    }
}

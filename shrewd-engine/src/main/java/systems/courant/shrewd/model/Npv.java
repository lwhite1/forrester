package systems.courant.shrewd.model;

import com.google.common.base.Preconditions;

import systems.courant.shrewd.model.compile.Resettable;

import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;

/**
 * Net present value accumulator that implements {@link Formula}, providing the standard
 * SD NPV builtin.
 *
 * <p>NPV accumulates the discounted present value of a stream of payments. At each
 * timestep, the current payment is discounted using an incrementally accumulated
 * discount factor to avoid overflow on long simulations:
 *
 * <pre>
 *     discount *= (1 + discount_rate)
 *     npv += stream / discount
 * </pre>
 *
 * <p>The discount rate is the fractional rate per timestep (e.g., 0.05 for 5% per step).
 * An optional factor multiplier is applied to each payment before discounting.
 *
 * <pre>{@code
 * Npv npv = Npv.of(() -> cashFlow.getValue(), 0.05, 1, sim::getCurrentStep);
 * }</pre>
 */
public class Npv implements Formula, Resettable {

    private final DoubleSupplier stream;
    private final double discountRate;
    private final double factor;
    private final IntSupplier currentStep;

    private double accumulated;
    private double cumulativeDiscount;
    private boolean initialized;
    private int lastStep = -1;

    private Npv(DoubleSupplier stream, double discountRate, double factor,
                IntSupplier currentStep) {
        Preconditions.checkNotNull(stream, "stream supplier must not be null");
        Preconditions.checkNotNull(currentStep, "currentStep supplier must not be null");
        Preconditions.checkArgument(discountRate > -1.0,
                "discountRate must be greater than -1.0, but got %s", discountRate);
        this.stream = stream;
        this.discountRate = discountRate;
        this.factor = factor;
        this.currentStep = currentStep;
    }

    /**
     * Creates an NPV formula with a factor of 1.
     *
     * @param stream       supplies the current payment value
     * @param discountRate the discount rate per timestep
     * @param currentStep  supplies the current simulation timestep
     * @return a new Npv formula
     */
    public static Npv of(DoubleSupplier stream, double discountRate,
                         IntSupplier currentStep) {
        return new Npv(stream, discountRate, 1.0, currentStep);
    }

    /**
     * Creates an NPV formula with a custom factor multiplier.
     *
     * @param stream       supplies the current payment value
     * @param discountRate the discount rate per timestep
     * @param factor       multiplier applied to each payment
     * @param currentStep  supplies the current simulation timestep
     * @return a new Npv formula
     */
    public static Npv of(DoubleSupplier stream, double discountRate, double factor,
                         IntSupplier currentStep) {
        return new Npv(stream, discountRate, factor, currentStep);
    }

    /**
     * Resets this Npv to its uninitialized state so it can be reused across simulation runs.
     */
    @Override
    public void reset() {
        accumulated = 0;
        cumulativeDiscount = 1.0;
        initialized = false;
        lastStep = -1;
    }

    /**
     * Returns the accumulated net present value through the current timestep.
     * Each new payment is discounted back to the initial timestep and added to the total.
     *
     * @return the cumulative discounted value
     */
    @Override
    public double getCurrentValue() {
        int step = currentStep.getAsInt();
        if (!initialized) {
            cumulativeDiscount = 1.0;
            accumulated = stream.getAsDouble() * factor;
            initialized = true;
            lastStep = step;
        } else if (step > lastStep) {
            int delta = step - lastStep;
            double discountMultiplier = 1 + discountRate;
            double streamVal = stream.getAsDouble();
            // Compound discount for all elapsed steps
            for (int d = 0; d < delta; d++) {
                cumulativeDiscount *= discountMultiplier;
            }
            // Add only the current step's payment at the final discount level
            accumulated += streamVal * factor / cumulativeDiscount;
            lastStep = step;
        }
        return accumulated;
    }
}

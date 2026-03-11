package systems.courant.shrewd.model;

/**
 * A function that computes the current value of a {@link Variable} each timestep.
 *
 * <p>Formula is the core abstraction for defining dynamic behavior. Every {@link Variable}
 * holds a Formula that the simulation engine calls once per timestep to obtain the variable's
 * current value.
 *
 * <p>Because Formula is a {@code @FunctionalInterface}, it can be provided as a lambda:
 *
 * <pre>{@code
 * // Inline lambda — most common usage
 * Variable birthRate = new Variable("Birth Rate", ItemUnits.PEOPLE,
 *         () -> population.getValue() * growthFraction.getValue());
 *
 * // Method reference
 * Variable demand = new Variable("Demand", ItemUnits.THING, sensor::readDemand);
 * }</pre>
 *
 * <p>For standard SD input functions, use the built-in implementations:
 * {@link Step}, {@link Ramp}, {@link Smooth}, and {@link Delay3}.
 *
 * @see Variable
 * @see Step
 * @see Ramp
 * @see Smooth
 * @see Delay3
 */
@FunctionalInterface
public interface Formula {

    /**
     * Computes and returns the current value of the variable that uses this formula.
     *
     * <p>Called once per simulation timestep. Implementations may read current stock levels,
     * other variable values, or constants. Avoid side effects — the simulation engine does
     * not guarantee evaluation order among variables within a single timestep.
     *
     * @return the computed value for the current timestep
     */
    double getCurrentValue();

}

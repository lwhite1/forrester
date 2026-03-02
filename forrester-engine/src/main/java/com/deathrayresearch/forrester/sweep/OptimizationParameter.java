package com.deathrayresearch.forrester.sweep;

/**
 * Defines a parameter to be optimized, including its name, bounds, and optional initial guess.
 *
 * @param name         the parameter name (used as a key in the map passed to the model factory)
 * @param lowerBound   the lower bound of the search space
 * @param upperBound   the upper bound of the search space
 * @param initialGuess the starting point for the optimizer, or {@link Double#NaN} to use the midpoint
 */
public record OptimizationParameter(String name, double lowerBound, double upperBound, double initialGuess) {

    /**
     * Creates a parameter with explicit bounds and initial guess.
     */
    public OptimizationParameter {
        if (lowerBound >= upperBound) {
            throw new IllegalArgumentException(
                    "lowerBound (" + lowerBound + ") must be less than upperBound (" + upperBound + ")");
        }
        if (!Double.isNaN(initialGuess) && (initialGuess < lowerBound || initialGuess > upperBound)) {
            throw new IllegalArgumentException(
                    "initialGuess (" + initialGuess + ") must be within bounds [" + lowerBound + ", " + upperBound + "]");
        }
    }

    /**
     * Creates a parameter with bounds only; the initial guess defaults to the midpoint.
     *
     * @param name       the parameter name
     * @param lowerBound the lower bound
     * @param upperBound the upper bound
     */
    public OptimizationParameter(String name, double lowerBound, double upperBound) {
        this(name, lowerBound, upperBound, Double.NaN);
    }

    /**
     * Returns the initial guess if explicitly set, otherwise the midpoint of the bounds.
     *
     * @return the effective initial guess
     */
    public double effectiveInitialGuess() {
        if (Double.isNaN(initialGuess)) {
            return (lowerBound + upperBound) / 2.0;
        }
        return initialGuess;
    }

    /**
     * Returns the range (upperBound - lowerBound) of this parameter.
     *
     * @return the parameter range
     */
    public double range() {
        return upperBound - lowerBound;
    }
}

package systems.courant.sd.sweep;

/**
 * Derivative-free optimization algorithms available for model calibration.
 */
public enum OptimizationAlgorithm {

    /**
     * Nelder-Mead simplex algorithm. Good general-purpose choice for low-dimensional problems.
     * Does not enforce parameter bounds — the model factory should handle out-of-bounds values
     * gracefully if possible.
     */
    NELDER_MEAD,

    /**
     * Bound Optimization BY Quadratic Approximation. Builds a quadratic interpolation model
     * within the feasible region. Requires at least 2 parameters.
     */
    BOBYQA,

    /**
     * Covariance Matrix Adaptation Evolution Strategy. A population-based algorithm well-suited
     * for higher-dimensional problems. Uses bounded search with configurable random seed.
     */
    CMAES
}

package systems.courant.forrester.sweep;

/**
 * A function that evaluates a simulation run and returns a scalar objective value.
 * Lower values are considered better (minimization).
 */
@FunctionalInterface
public interface ObjectiveFunction {

    /**
     * Evaluates the given simulation run result.
     *
     * @param runResult the result of a single simulation run
     * @return a scalar objective value (lower is better)
     */
    double evaluate(RunResult runResult);
}

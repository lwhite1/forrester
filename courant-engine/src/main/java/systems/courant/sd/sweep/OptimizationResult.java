package systems.courant.sd.sweep;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The result of an optimization run, containing the best parameters found, the objective
 * value achieved, the full simulation result for the best evaluation, and the total
 * number of objective function evaluations.
 */
public class OptimizationResult {

    private final Map<String, Double> bestParameters;
    private final double bestObjectiveValue;
    private final RunResult bestRunResult;
    private final int evaluationCount;

    /**
     * Creates a new optimization result.
     *
     * @param bestParameters     the parameter values that produced the best objective value
     * @param bestObjectiveValue the best (lowest) objective value found
     * @param bestRunResult      the simulation run result for the best evaluation
     * @param evaluationCount    the total number of objective function evaluations performed
     */
    public OptimizationResult(Map<String, Double> bestParameters, double bestObjectiveValue,
                              RunResult bestRunResult, int evaluationCount) {
        this.bestParameters = Collections.unmodifiableMap(new LinkedHashMap<>(bestParameters));
        this.bestObjectiveValue = bestObjectiveValue;
        this.bestRunResult = bestRunResult;
        this.evaluationCount = evaluationCount;
    }

    /**
     * Returns the parameter values that produced the best objective value.
     *
     * @return an unmodifiable map of parameter name to optimal value
     */
    public Map<String, Double> getBestParameters() {
        return bestParameters;
    }

    /**
     * Returns the best (lowest) objective value found during optimization.
     *
     * @return the best objective value
     */
    public double getBestObjectiveValue() {
        return bestObjectiveValue;
    }

    /**
     * Returns the full simulation run result from the evaluation that produced the best
     * objective value.
     *
     * @return the best run result
     */
    public RunResult getBestRunResult() {
        return bestRunResult;
    }

    /**
     * Returns the total number of objective function evaluations performed.
     *
     * @return the evaluation count
     */
    public int getEvaluationCount() {
        return evaluationCount;
    }
}

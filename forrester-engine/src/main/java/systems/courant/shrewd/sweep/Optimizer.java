package systems.courant.shrewd.sweep;

import systems.courant.shrewd.Simulation;
import systems.courant.shrewd.measure.Quantity;
import systems.courant.shrewd.measure.TimeUnit;
import systems.courant.shrewd.model.Model;
import systems.courant.shrewd.model.compile.CompiledModel;

import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.optim.InitialGuess;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.SimpleBounds;
import org.apache.commons.math3.optim.SimpleValueChecker;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.BOBYQAOptimizer;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.CMAESOptimizer;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.NelderMeadSimplex;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.SimplexOptimizer;
import org.apache.commons.math3.random.MersenneTwister;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Finds parameter values that minimize an objective function evaluated on simulation output.
 * Wraps Apache Commons Math derivative-free optimizers and adapts them to the Forrester
 * simulation framework.
 *
 * <pre>{@code
 * OptimizationResult result = Optimizer.builder()
 *     .parameter("Contact Rate", 1.0, 20.0)
 *     .parameter("Infectivity", 0.01, 0.50)
 *     .modelFactory(params -> buildSirModel(
 *             params.get("Contact Rate"), params.get("Infectivity")))
 *     .objective(Objectives.fitToTimeSeries("Infectious", observedData))
 *     .algorithm(OptimizationAlgorithm.NELDER_MEAD)
 *     .maxEvaluations(500)
 *     .timeStep(DAY)
 *     .duration(Times.weeks(8))
 *     .build()
 *     .execute();
 * }</pre>
 */
public class Optimizer {

    private final List<OptimizationParameter> parameters;
    private final Function<Map<String, Double>, Model> modelFactory;
    private final Function<Map<String, Double>, CompiledModel> compiledModelFactory;
    private final ObjectiveFunction objective;
    private final OptimizationAlgorithm algorithm;
    private final int maxEvaluations;
    private final TimeUnit timeStep;
    private final Quantity duration;
    private final long seed;

    private Optimizer(Builder builder) {
        this.parameters = builder.parameters;
        this.modelFactory = builder.modelFactory;
        this.compiledModelFactory = builder.compiledModelFactory;
        this.objective = builder.objective;
        this.algorithm = builder.algorithm;
        this.maxEvaluations = builder.maxEvaluations;
        this.timeStep = builder.timeStep;
        this.duration = builder.duration;
        this.seed = builder.seed;
    }

    /**
     * Runs the optimization and returns the best result found.
     *
     * @return the optimization result
     */
    public OptimizationResult execute() {
        int n = parameters.size();

        // Best-tracking state
        double[] bestObjective = {Double.MAX_VALUE};
        Map<String, Double>[] bestParams = new Map[]{null};
        RunResult[] bestRun = {null};
        int[] evalCount = {0};

        MultivariateFunction adapter = point -> {
            evalCount[0]++;

            // Clamp parameters to bounds (Nelder-Mead does not enforce bounds natively)
            Map<String, Double> paramMap = new LinkedHashMap<>();
            for (int i = 0; i < n; i++) {
                double v = Math.max(parameters.get(i).lowerBound(),
                        Math.min(parameters.get(i).upperBound(), point[i]));
                paramMap.put(parameters.get(i).name(), v);
            }

            Simulation simulation;
            if (compiledModelFactory != null) {
                CompiledModel compiled = compiledModelFactory.apply(paramMap);
                simulation = compiled.createSimulation(timeStep, duration);
            } else {
                Model model = modelFactory.apply(paramMap);
                simulation = new Simulation(model, timeStep, duration);
            }
            RunResult runResult = new RunResult(paramMap);

            simulation.addEventHandler(runResult);
            simulation.execute();

            double value = objective.evaluate(runResult);

            // Always capture the first run to prevent null bestRun in fallback
            if (bestRun[0] == null) {
                bestRun[0] = runResult;
            }
            if (value < bestObjective[0]) {
                bestObjective[0] = value;
                bestParams[0] = paramMap;
                bestRun[0] = runResult;
            }

            return value;
        };

        double[] initialGuess = new double[n];
        for (int i = 0; i < n; i++) {
            initialGuess[i] = parameters.get(i).effectiveInitialGuess();
        }

        switch (algorithm) {
            case NELDER_MEAD -> executeNelderMead(adapter, initialGuess, n);
            case BOBYQA -> executeBobyqa(adapter, initialGuess, n);
            case CMAES -> executeCmaes(adapter, initialGuess, n);
        }

        // Guard against no improvement (all evaluations returned MAX_VALUE or NaN)
        if (bestParams[0] == null) {
            Map<String, Double> fallback = new LinkedHashMap<>();
            for (int i = 0; i < n; i++) {
                fallback.put(parameters.get(i).name(), initialGuess[i]);
            }
            return new OptimizationResult(fallback, bestObjective[0], bestRun[0], evalCount[0]);
        }
        return new OptimizationResult(bestParams[0], bestObjective[0], bestRun[0], evalCount[0]);
    }

    private void executeNelderMead(MultivariateFunction adapter, double[] initialGuess, int n) {
        double[] steps = new double[n];
        for (int i = 0; i < n; i++) {
            steps[i] = parameters.get(i).range() * 0.10;
        }

        SimplexOptimizer optimizer = new SimplexOptimizer(new SimpleValueChecker(1e-10, 1e-10));
        optimizer.optimize(
                new MaxEval(maxEvaluations),
                new org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction(adapter),
                GoalType.MINIMIZE,
                new InitialGuess(initialGuess),
                new NelderMeadSimplex(steps)
        );
    }

    private void executeBobyqa(MultivariateFunction adapter, double[] initialGuess, int n) {
        double[] lower = new double[n];
        double[] upper = new double[n];
        for (int i = 0; i < n; i++) {
            lower[i] = parameters.get(i).lowerBound();
            upper[i] = parameters.get(i).upperBound();
        }

        int interpolationPoints = 2 * n + 1;
        BOBYQAOptimizer optimizer = new BOBYQAOptimizer(interpolationPoints);
        optimizer.optimize(
                new MaxEval(maxEvaluations),
                new org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction(adapter),
                GoalType.MINIMIZE,
                new InitialGuess(initialGuess),
                new SimpleBounds(lower, upper)
        );
    }

    private void executeCmaes(MultivariateFunction adapter, double[] initialGuess, int n) {
        double[] lower = new double[n];
        double[] upper = new double[n];
        double[] sigma = new double[n];
        for (int i = 0; i < n; i++) {
            lower[i] = parameters.get(i).lowerBound();
            upper[i] = parameters.get(i).upperBound();
            sigma[i] = parameters.get(i).range() * 0.25;
        }

        int populationSize = 4 + (int) Math.floor(3.0 * Math.log(n));

        CMAESOptimizer optimizer = new CMAESOptimizer(
                maxEvaluations,
                1e-10,
                true,
                0,
                10,
                new MersenneTwister(seed),
                false,
                new SimpleValueChecker(1e-10, 1e-10)
        );

        optimizer.optimize(
                new MaxEval(maxEvaluations),
                new org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction(adapter),
                GoalType.MINIMIZE,
                new InitialGuess(initialGuess),
                new SimpleBounds(lower, upper),
                new CMAESOptimizer.Sigma(sigma),
                new CMAESOptimizer.PopulationSize(populationSize)
        );
    }

    /**
     * Returns a new builder for configuring an optimization run.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link Optimizer}.
     */
    public static class Builder {

        private final List<OptimizationParameter> parameters = new ArrayList<>();
        private Function<Map<String, Double>, Model> modelFactory;
        private Function<Map<String, Double>, CompiledModel> compiledModelFactory;
        private ObjectiveFunction objective;
        private OptimizationAlgorithm algorithm = OptimizationAlgorithm.NELDER_MEAD;
        private int maxEvaluations = 1000;
        private TimeUnit timeStep;
        private Quantity duration;
        private long seed = 12345L;

        private Builder() {
        }

        /**
         * Adds a parameter with bounds. The initial guess defaults to the midpoint.
         *
         * @param name       the parameter name
         * @param lowerBound the lower bound
         * @param upperBound the upper bound
         * @return this builder
         */
        public Builder parameter(String name, double lowerBound, double upperBound) {
            parameters.add(new OptimizationParameter(name, lowerBound, upperBound));
            return this;
        }

        /**
         * Adds a parameter with bounds and an explicit initial guess.
         *
         * @param name         the parameter name
         * @param lowerBound   the lower bound
         * @param upperBound   the upper bound
         * @param initialGuess the starting point for the optimizer
         * @return this builder
         */
        public Builder parameter(String name, double lowerBound, double upperBound, double initialGuess) {
            parameters.add(new OptimizationParameter(name, lowerBound, upperBound, initialGuess));
            return this;
        }

        /**
         * Sets the factory function that builds a fresh model from a parameter map.
         *
         * @param modelFactory a function that receives a map of parameter name to value
         * @return this builder
         */
        public Builder modelFactory(Function<Map<String, Double>, Model> modelFactory) {
            this.modelFactory = modelFactory;
            return this;
        }

        /**
         * Sets the factory function that builds a fresh compiled model from a parameter map.
         * Compiled models use {@link CompiledModel#createSimulation} to install step synchronization,
         * which is required for time-dependent functions (STEP, RAMP, PULSE, SMOOTH, TIME, DT).
         *
         * @param compiledModelFactory a function that receives a map of parameter name to value
         * @return this builder
         */
        public Builder compiledModelFactory(
                Function<Map<String, Double>, CompiledModel> compiledModelFactory) {
            this.compiledModelFactory = compiledModelFactory;
            return this;
        }

        /**
         * Sets the objective function to minimize.
         *
         * @param objective the objective function
         * @return this builder
         */
        public Builder objective(ObjectiveFunction objective) {
            this.objective = objective;
            return this;
        }

        /**
         * Sets the optimization algorithm. Default is {@link OptimizationAlgorithm#NELDER_MEAD}.
         *
         * @param algorithm the algorithm to use
         * @return this builder
         */
        public Builder algorithm(OptimizationAlgorithm algorithm) {
            this.algorithm = algorithm;
            return this;
        }

        /**
         * Sets the maximum number of objective function evaluations. Default is 1000.
         *
         * @param maxEvaluations the maximum evaluations
         * @return this builder
         */
        public Builder maxEvaluations(int maxEvaluations) {
            this.maxEvaluations = maxEvaluations;
            return this;
        }

        /**
         * Sets the simulation time step.
         *
         * @param timeStep the time unit for each step
         * @return this builder
         */
        public Builder timeStep(TimeUnit timeStep) {
            this.timeStep = timeStep;
            return this;
        }

        /**
         * Sets the simulation duration.
         *
         * @param duration the total simulation time
         * @return this builder
         */
        public Builder duration(Quantity duration) {
            this.duration = duration;
            return this;
        }

        /**
         * Sets the RNG seed for stochastic algorithms (CMA-ES). Default is 12345.
         *
         * @param seed the random number generator seed
         * @return this builder
         */
        public Builder seed(long seed) {
            this.seed = seed;
            return this;
        }

        /**
         * Builds the {@link Optimizer} instance.
         *
         * @return a configured Optimizer ready to execute
         * @throws IllegalStateException if any required field is missing or constraints are violated
         */
        public Optimizer build() {
            if (parameters.isEmpty()) {
                throw new IllegalStateException("At least one parameter is required");
            }
            if (modelFactory == null && compiledModelFactory == null) {
                throw new IllegalStateException(
                        "Either modelFactory or compiledModelFactory is required");
            }
            if (objective == null) {
                throw new IllegalStateException("objective is required");
            }
            if (timeStep == null) {
                throw new IllegalStateException("timeStep is required");
            }
            if (duration == null) {
                throw new IllegalStateException("duration is required");
            }
            if (maxEvaluations < 1) {
                throw new IllegalStateException("maxEvaluations must be at least 1");
            }
            if (algorithm == OptimizationAlgorithm.BOBYQA && parameters.size() < 2) {
                throw new IllegalStateException("BOBYQA requires at least 2 parameters");
            }
            return new Optimizer(this);
        }
    }
}

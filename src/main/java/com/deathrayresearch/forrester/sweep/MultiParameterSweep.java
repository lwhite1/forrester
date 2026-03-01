package com.deathrayresearch.forrester.sweep;

import com.deathrayresearch.forrester.Simulation;
import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.TimeUnit;
import com.deathrayresearch.forrester.model.Model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Runs a model over the Cartesian product of multiple parameter arrays and collects results.
 * Each combination gets a fresh model instance built by the supplied factory function.
 *
 * <pre>{@code
 * MultiSweepResult result = MultiParameterSweep.builder()
 *     .parameter("Contact Rate", ParameterSweep.linspace(2.0, 14.0, 4.0))
 *     .parameter("Infectivity", new double[]{0.05, 0.10, 0.15})
 *     .modelFactory(params -> buildModel(params.get("Contact Rate"), params.get("Infectivity")))
 *     .timeStep(DAY)
 *     .duration(Times.weeks(8))
 *     .build()
 *     .execute();
 * }</pre>
 */
public class MultiParameterSweep {

    private final Map<String, double[]> parameters;
    private final Function<Map<String, Double>, Model> modelFactory;
    private final TimeUnit timeStep;
    private final Quantity duration;

    private MultiParameterSweep(Builder builder) {
        this.parameters = builder.parameters;
        this.modelFactory = builder.modelFactory;
        this.timeStep = builder.timeStep;
        this.duration = builder.duration;
    }

    /**
     * Executes the sweep: computes the Cartesian product of all parameter arrays, builds
     * a fresh model for each combination, runs the simulation, and collects the results.
     *
     * @return a {@link MultiSweepResult} containing one {@link RunResult} per combination
     */
    public MultiSweepResult execute() {
        List<String> paramNames = new ArrayList<>(parameters.keySet());
        List<double[]> paramArrays = new ArrayList<>(parameters.values());
        List<Map<String, Double>> combinations = cartesianProduct(paramNames, paramArrays);

        List<RunResult> results = new ArrayList<>();

        for (Map<String, Double> paramMap : combinations) {
            Model model = modelFactory.apply(paramMap);
            RunResult runResult = new RunResult(paramMap);

            Simulation simulation = new Simulation(model, timeStep, duration);
            simulation.addEventHandler(runResult);
            simulation.execute();

            results.add(runResult);
        }

        return new MultiSweepResult(paramNames, results);
    }

    /**
     * Computes the Cartesian product of the parameter arrays, returning one map per combination.
     */
    private static List<Map<String, Double>> cartesianProduct(List<String> names,
                                                               List<double[]> arrays) {
        List<Map<String, Double>> result = new ArrayList<>();
        result.add(new LinkedHashMap<>());

        for (int i = 0; i < names.size(); i++) {
            String name = names.get(i);
            double[] values = arrays.get(i);
            List<Map<String, Double>> expanded = new ArrayList<>();

            for (Map<String, Double> existing : result) {
                for (double value : values) {
                    Map<String, Double> copy = new LinkedHashMap<>(existing);
                    copy.put(name, value);
                    expanded.add(copy);
                }
            }

            result = expanded;
        }

        return result;
    }

    /**
     * Returns a new builder for configuring a multi-parameter sweep.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link MultiParameterSweep}.
     */
    public static class Builder {

        private final Map<String, double[]> parameters = new LinkedHashMap<>();
        private Function<Map<String, Double>, Model> modelFactory;
        private TimeUnit timeStep;
        private Quantity duration;

        private Builder() {
        }

        /**
         * Adds a parameter with its sweep values.
         *
         * @param name   the parameter name (used as a key in the map passed to the model factory)
         * @param values the values to sweep over
         * @return this builder
         */
        public Builder parameter(String name, double[] values) {
            this.parameters.put(name, values);
            return this;
        }

        /**
         * Sets the factory function that builds a fresh model from a parameter vector.
         *
         * @param modelFactory a function that receives a map of parameter name to value
         * @return this builder
         */
        public Builder modelFactory(Function<Map<String, Double>, Model> modelFactory) {
            this.modelFactory = modelFactory;
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
         * Builds the {@link MultiParameterSweep} instance.
         *
         * @return a configured MultiParameterSweep ready to execute
         * @throws IllegalStateException if any required field is missing
         */
        public MultiParameterSweep build() {
            if (parameters.isEmpty()) {
                throw new IllegalStateException("At least one parameter is required");
            }
            for (Map.Entry<String, double[]> entry : parameters.entrySet()) {
                if (entry.getValue() == null || entry.getValue().length == 0) {
                    throw new IllegalStateException(
                            "Parameter '" + entry.getKey() + "' must have at least one value");
                }
            }
            if (modelFactory == null) {
                throw new IllegalStateException("modelFactory is required");
            }
            if (timeStep == null) {
                throw new IllegalStateException("timeStep is required");
            }
            if (duration == null) {
                throw new IllegalStateException("duration is required");
            }
            return new MultiParameterSweep(this);
        }
    }
}

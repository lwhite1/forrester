package systems.courant.forrester.sweep;

import systems.courant.forrester.Simulation;
import systems.courant.forrester.measure.Quantity;
import systems.courant.forrester.measure.TimeUnit;
import systems.courant.forrester.model.Model;
import systems.courant.forrester.model.compile.CompiledModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.DoubleFunction;

/**
 * Runs a model multiple times with different parameter values and collects the results.
 * Each run gets a fresh model instance built by the supplied factory function,
 * avoiding shared mutable state.
 *
 * <pre>{@code
 * SweepResult result = ParameterSweep.builder()
 *     .parameterName("Contact Rate")
 *     .parameterValues(ParameterSweep.linspace(2.0, 14.0, 2.0))
 *     .modelFactory(this::buildSirModel)
 *     .timeStep(DAY)
 *     .duration(Times.weeks(8))
 *     .build()
 *     .execute();
 * }</pre>
 */
public class ParameterSweep {

    private final String parameterName;
    private final double[] parameterValues;
    private final DoubleFunction<Model> modelFactory;
    private final DoubleFunction<CompiledModel> compiledModelFactory;
    private final TimeUnit timeStep;
    private final Quantity duration;

    private ParameterSweep(Builder builder) {
        this.parameterName = builder.parameterName;
        this.parameterValues = builder.parameterValues;
        this.modelFactory = builder.modelFactory;
        this.compiledModelFactory = builder.compiledModelFactory;
        this.timeStep = builder.timeStep;
        this.duration = builder.duration;
    }

    /**
     * Executes the sweep: for each parameter value, builds a fresh model, runs a simulation,
     * and collects the results.
     *
     * @return a {@link SweepResult} containing one {@link RunResult} per parameter value
     */
    public SweepResult execute() {
        List<RunResult> results = new ArrayList<>();

        for (double value : parameterValues) {
            Simulation simulation;
            if (compiledModelFactory != null) {
                CompiledModel compiled = compiledModelFactory.apply(value);
                simulation = compiled.createSimulation(timeStep, duration);
            } else {
                Model model = modelFactory.apply(value);
                simulation = new Simulation(model, timeStep, duration);
            }
            RunResult runResult = new RunResult(Map.of(parameterName, value));

            simulation.addEventHandler(runResult);
            simulation.execute();

            results.add(runResult);
        }

        return new SweepResult(parameterName, results);
    }

    /**
     * Returns a new builder for configuring a parameter sweep.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Generates an array of evenly spaced values from {@code start} to {@code end} (inclusive)
     * with the given step size. Uses {@code start + i * step} to avoid floating-point drift.
     *
     * @param start the first value
     * @param end   the upper bound (inclusive if reachable by step)
     * @param step  the increment between values
     * @return an array of parameter values
     */
    public static double[] linspace(double start, double end, double step) {
        if (step <= 0) {
            throw new IllegalArgumentException("step must be positive, but got " + step);
        }
        if (end < start) {
            throw new IllegalArgumentException(
                    "end (" + end + ") must be >= start (" + start + ")");
        }
        List<Double> values = new ArrayList<>();
        int count = (int) Math.floor((end - start) / step);
        for (int i = 0; i <= count; i++) {
            values.add(start + i * step);
        }
        double[] result = new double[values.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = values.get(i);
        }
        return result;
    }

    /**
     * Builder for {@link ParameterSweep}.
     */
    public static class Builder {

        private String parameterName;
        private double[] parameterValues;
        private DoubleFunction<Model> modelFactory;
        private DoubleFunction<CompiledModel> compiledModelFactory;
        private TimeUnit timeStep;
        private Quantity duration;

        private Builder() {
        }

        /**
         * Sets the name of the parameter being swept.
         *
         * @param parameterName the parameter name
         * @return this builder
         */
        public Builder parameterName(String parameterName) {
            this.parameterName = parameterName;
            return this;
        }

        /**
         * Sets the array of values to sweep over. Use {@link ParameterSweep#linspace}
         * to generate evenly spaced values.
         *
         * @param parameterValues the parameter values to test
         * @return this builder
         */
        public Builder parameterValues(double[] parameterValues) {
            this.parameterValues = parameterValues;
            return this;
        }

        /**
         * Sets the factory function that builds a fresh model for each parameter value.
         *
         * @param modelFactory a function that receives the parameter value and returns a new model
         * @return this builder
         */
        public Builder modelFactory(DoubleFunction<Model> modelFactory) {
            this.modelFactory = modelFactory;
            return this;
        }

        /**
         * Sets the factory function that builds a fresh compiled model for each parameter value.
         * Compiled models use {@link CompiledModel#createSimulation} to install step synchronization,
         * which is required for time-dependent functions (STEP, RAMP, PULSE, SMOOTH, TIME, DT).
         *
         * @param compiledModelFactory a function that receives the parameter value
         * @return this builder
         */
        public Builder compiledModelFactory(DoubleFunction<CompiledModel> compiledModelFactory) {
            this.compiledModelFactory = compiledModelFactory;
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
         * Builds the {@link ParameterSweep} instance.
         *
         * @return a configured ParameterSweep ready to execute
         * @throws IllegalStateException if any required field is missing
         */
        public ParameterSweep build() {
            if (parameterName == null) {
                throw new IllegalStateException("parameterName is required");
            }
            if (parameterValues == null || parameterValues.length == 0) {
                throw new IllegalStateException("parameterValues is required and must not be empty");
            }
            if (modelFactory == null && compiledModelFactory == null) {
                throw new IllegalStateException(
                        "Either modelFactory or compiledModelFactory is required");
            }
            if (timeStep == null) {
                throw new IllegalStateException("timeStep is required");
            }
            if (duration == null) {
                throw new IllegalStateException("duration is required");
            }
            return new ParameterSweep(this);
        }
    }
}

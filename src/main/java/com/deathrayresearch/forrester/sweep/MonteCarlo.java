package com.deathrayresearch.forrester.sweep;

import com.deathrayresearch.forrester.Simulation;
import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.TimeUnit;
import com.deathrayresearch.forrester.model.Model;

import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.commons.math3.random.MersenneTwister;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Runs a model many times with randomly sampled parameter values to quantify uncertainty.
 * Each iteration draws a parameter vector from the configured distributions, builds a fresh
 * model, runs the simulation, and collects the result.
 *
 * <p>Supports pure random sampling and Latin Hypercube Sampling (LHS) for better coverage
 * of the parameter space with fewer iterations.
 *
 * <pre>{@code
 * MonteCarloResult result = MonteCarlo.builder()
 *     .parameter("Contact Rate", new NormalDistribution(8, 2))
 *     .parameter("Infectivity", new UniformRealDistribution(0.05, 0.15))
 *     .modelFactory(params -> buildModel(params.get("Contact Rate"), params.get("Infectivity")))
 *     .iterations(200)
 *     .sampling(SamplingMethod.LATIN_HYPERCUBE)
 *     .seed(42L)
 *     .timeStep(DAY)
 *     .duration(Times.weeks(8))
 *     .build()
 *     .execute();
 * }</pre>
 */
public class MonteCarlo {

    private final Map<String, RealDistribution> parameters;
    private final Function<Map<String, Double>, Model> modelFactory;
    private final int iterations;
    private final SamplingMethod samplingMethod;
    private final long seed;
    private final TimeUnit timeStep;
    private final Quantity duration;

    private MonteCarlo(Builder builder) {
        this.parameters = builder.parameters;
        this.modelFactory = builder.modelFactory;
        this.iterations = builder.iterations;
        this.samplingMethod = builder.samplingMethod;
        this.seed = builder.seed;
        this.timeStep = builder.timeStep;
        this.duration = builder.duration;
    }

    /**
     * Executes the Monte Carlo simulation: samples parameter vectors, builds a model
     * for each, runs the simulation, and aggregates results.
     *
     * @return a {@link MonteCarloResult} containing one {@link RunResult} per iteration
     */
    public MonteCarloResult execute() {
        MersenneTwister rng = new MersenneTwister(seed);
        double[][] parameterVectors = generateParameterVectors(rng);

        List<String> paramNames = new ArrayList<>(parameters.keySet());
        List<RunResult> results = new ArrayList<>();

        for (int i = 0; i < iterations; i++) {
            Map<String, Double> paramMap = new LinkedHashMap<>();
            for (int p = 0; p < paramNames.size(); p++) {
                paramMap.put(paramNames.get(p), parameterVectors[i][p]);
            }

            Model model = modelFactory.apply(paramMap);
            RunResult runResult = new RunResult(i);

            Simulation simulation = new Simulation(model, timeStep, duration);
            simulation.addEventHandler(runResult);
            simulation.execute();

            results.add(runResult);
        }

        return new MonteCarloResult(results);
    }

    /**
     * Generates an N x P matrix of sampled parameter values, where N is the number of
     * iterations and P is the number of parameters.
     */
    private double[][] generateParameterVectors(MersenneTwister rng) {
        List<RealDistribution> distributions = new ArrayList<>(parameters.values());
        int paramCount = distributions.size();

        if (samplingMethod == SamplingMethod.LATIN_HYPERCUBE) {
            return generateLatinHypercube(rng, distributions, paramCount);
        } else {
            return generateRandom(rng, distributions, paramCount);
        }
    }

    /**
     * Pure random sampling: each parameter value is an independent draw from its distribution.
     */
    private double[][] generateRandom(MersenneTwister rng, List<RealDistribution> distributions,
                                      int paramCount) {
        double[][] vectors = new double[iterations][paramCount];
        for (int i = 0; i < iterations; i++) {
            for (int p = 0; p < paramCount; p++) {
                RealDistribution dist = distributions.get(p);
                dist.reseedRandomGenerator(rng.nextLong());
                vectors[i][p] = dist.sample();
            }
        }
        return vectors;
    }

    /**
     * Latin Hypercube Sampling: divides [0,1] into N equal strata per parameter,
     * draws one point per stratum, then shuffles columns independently to break correlation.
     * Maps uniform samples through each distribution's inverse CDF.
     */
    private double[][] generateLatinHypercube(MersenneTwister rng,
                                              List<RealDistribution> distributions,
                                              int paramCount) {
        double[][] vectors = new double[iterations][paramCount];

        for (int p = 0; p < paramCount; p++) {
            RealDistribution dist = distributions.get(p);

            // Generate one stratified sample per stratum
            double[] column = new double[iterations];
            for (int i = 0; i < iterations; i++) {
                double u = (i + rng.nextDouble()) / iterations;
                column[i] = dist.inverseCumulativeProbability(u);
            }

            // Fisher-Yates shuffle
            for (int i = iterations - 1; i > 0; i--) {
                int j = rng.nextInt(i + 1);
                double temp = column[i];
                column[i] = column[j];
                column[j] = temp;
            }

            for (int i = 0; i < iterations; i++) {
                vectors[i][p] = column[i];
            }
        }

        return vectors;
    }

    /**
     * Returns a new builder for configuring a Monte Carlo simulation.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link MonteCarlo}.
     */
    public static class Builder {

        private final Map<String, RealDistribution> parameters = new LinkedHashMap<>();
        private Function<Map<String, Double>, Model> modelFactory;
        private int iterations = 200;
        private SamplingMethod samplingMethod = SamplingMethod.LATIN_HYPERCUBE;
        private long seed = 12345L;
        private TimeUnit timeStep;
        private Quantity duration;

        private Builder() {
        }

        /**
         * Adds a parameter with its probability distribution.
         *
         * @param name         the parameter name (used as a key in the map passed to the model factory)
         * @param distribution the probability distribution to sample from
         * @return this builder
         */
        public Builder parameter(String name, RealDistribution distribution) {
            this.parameters.put(name, distribution);
            return this;
        }

        /**
         * Sets the factory function that builds a fresh model from a parameter vector.
         *
         * @param modelFactory a function that receives a map of parameter name to sampled value
         * @return this builder
         */
        public Builder modelFactory(Function<Map<String, Double>, Model> modelFactory) {
            this.modelFactory = modelFactory;
            return this;
        }

        /**
         * Sets the number of simulation runs. Default is 200.
         *
         * @param iterations the number of iterations
         * @return this builder
         */
        public Builder iterations(int iterations) {
            this.iterations = iterations;
            return this;
        }

        /**
         * Sets the sampling method. Default is {@link SamplingMethod#LATIN_HYPERCUBE}.
         *
         * @param samplingMethod the sampling strategy
         * @return this builder
         */
        public Builder sampling(SamplingMethod samplingMethod) {
            this.samplingMethod = samplingMethod;
            return this;
        }

        /**
         * Sets the RNG seed for reproducibility. Default is 12345.
         *
         * @param seed the random number generator seed
         * @return this builder
         */
        public Builder seed(long seed) {
            this.seed = seed;
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
         * Builds the {@link MonteCarlo} instance.
         *
         * @return a configured MonteCarlo ready to execute
         * @throws IllegalStateException if any required field is missing
         */
        public MonteCarlo build() {
            if (parameters.isEmpty()) {
                throw new IllegalStateException("At least one parameter is required");
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
            if (iterations < 1) {
                throw new IllegalStateException("iterations must be at least 1");
            }
            return new MonteCarlo(this);
        }
    }
}

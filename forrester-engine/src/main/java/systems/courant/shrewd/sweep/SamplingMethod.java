package systems.courant.shrewd.sweep;

/**
 * Sampling strategy for Monte Carlo simulation.
 */
public enum SamplingMethod {

    /**
     * Pure Monte Carlo — independent random draws from each distribution.
     */
    RANDOM,

    /**
     * Latin Hypercube Sampling — stratified sampling that provides better coverage
     * of the parameter space with fewer iterations.
     */
    LATIN_HYPERCUBE
}

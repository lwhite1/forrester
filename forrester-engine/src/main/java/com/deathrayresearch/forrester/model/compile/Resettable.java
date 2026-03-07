package com.deathrayresearch.forrester.model.compile;

/**
 * Interface for stateful formula components that need to be reset between simulation runs.
 */
@FunctionalInterface
public interface Resettable {

    /**
     * Resets this component to its initial state so the model can be re-simulated.
     */
    void reset();
}

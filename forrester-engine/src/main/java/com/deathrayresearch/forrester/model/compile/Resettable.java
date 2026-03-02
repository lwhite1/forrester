package com.deathrayresearch.forrester.model.compile;

/**
 * Interface for stateful formula components that need to be reset between simulation runs.
 */
@FunctionalInterface
public interface Resettable {

    void reset();
}

package com.deathrayresearch.forrester.model.flows;

import com.deathrayresearch.forrester.model.Flow;

import static com.deathrayresearch.forrester.measure.Units.SECOND;

/**
 * A rate specified as a quantity per second
 */
public abstract class FlowPerSecond extends Flow {

    /**
     * Creates a new per-second flow with the given name.
     *
     * @param name the flow name
     */
    public FlowPerSecond(String name) {
        super(name, SECOND);
    }
}

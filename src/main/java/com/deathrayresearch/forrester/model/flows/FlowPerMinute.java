package com.deathrayresearch.forrester.model.flows;

import com.deathrayresearch.forrester.model.Flow;

import static com.deathrayresearch.forrester.measure.Units.MINUTE;

/**
 * A rate specified as a quantity per minute
 */
public abstract class FlowPerMinute extends Flow {

    /**
     * Creates a new per-minute flow with the given name.
     *
     * @param name the flow name
     */
    public FlowPerMinute(String name) {
        super(name, MINUTE);
    }
}

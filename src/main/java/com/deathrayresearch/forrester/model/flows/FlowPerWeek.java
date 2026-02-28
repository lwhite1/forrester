package com.deathrayresearch.forrester.model.flows;

import com.deathrayresearch.forrester.model.Flow;

import static com.deathrayresearch.forrester.measure.Units.WEEK;

/**
 * A rate specified as a quantity per week
 */
public abstract class FlowPerWeek extends Flow {

    /**
     * Creates a new weekly flow with the given name.
     *
     * @param name the flow name
     */
    public FlowPerWeek(String name) {
        super(name, WEEK);
    }
}

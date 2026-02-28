package com.deathrayresearch.forrester.model.flows;

import com.deathrayresearch.forrester.model.Flow;

import static com.deathrayresearch.forrester.measure.Units.HOUR;

/**
 * A rate specified as a quantity per hour
 */
public abstract class FlowPerHour extends Flow {

    /**
     * Creates a new hourly flow with the given name.
     *
     * @param name the flow name
     */
    public FlowPerHour(String name) {
        super(name, HOUR);
    }
}

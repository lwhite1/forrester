package com.deathrayresearch.forrester.model.flows;

import com.deathrayresearch.forrester.model.Flow;

import static com.deathrayresearch.forrester.measure.Units.DAY;

/**
 * A rate specified as a quantity per day (a.k.a. a daily rate)
 */
public abstract class FlowPerDay extends Flow {

    /**
     * Creates a new daily flow with the given name.
     *
     * @param name the flow name
     */
    public FlowPerDay(String name) {
        super(name, DAY);
    }
}

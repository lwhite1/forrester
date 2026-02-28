package com.deathrayresearch.forrester.model.flows;

import com.deathrayresearch.forrester.model.Flow;

import static com.deathrayresearch.forrester.measure.Units.HOUR;

/**
 * A rate specified as a quantity per hour
 */
public abstract class FlowPerHour extends Flow {

    public FlowPerHour(String name) {
        super(name, HOUR);
    }
}

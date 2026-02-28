package com.deathrayresearch.forrester.model.flows;

import com.deathrayresearch.forrester.model.Flow;

import static com.deathrayresearch.forrester.measure.Units.DAY;

/**
 * A rate specified as a quantity per day (a.k.a. a daily rate)
 *
 * @deprecated Use {@link Flow#create(String, com.deathrayresearch.forrester.measure.TimeUnit,
 *     java.util.function.Supplier)} or the factory methods in
 *     {@link com.deathrayresearch.forrester.model.Flows} instead.
 */
@Deprecated
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

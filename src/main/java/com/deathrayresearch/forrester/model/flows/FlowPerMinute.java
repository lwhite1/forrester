package com.deathrayresearch.forrester.model.flows;

import com.deathrayresearch.forrester.model.Flow;

import static com.deathrayresearch.forrester.measure.Units.MINUTE;

/**
 * A rate specified as a quantity per minute
 *
 * @deprecated Use {@link Flow#create(String, com.deathrayresearch.forrester.measure.TimeUnit,
 *     java.util.function.Supplier)} or the factory methods in
 *     {@link com.deathrayresearch.forrester.model.Flows} instead.
 */
@Deprecated
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

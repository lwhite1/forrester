package com.deathrayresearch.forrester.model.flows;

import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.model.Flow;

import static com.deathrayresearch.forrester.measure.Units.SECOND;

/**
 * A rate specified as a quantity per second
 */
public abstract class FlowPerSecond extends Flow {

    public FlowPerSecond(String name) {
        super(name, SECOND);
    }

    @Override
    protected Quantity quantityPerTimeUnit() {
        return quantityPerSecond();
    }

    protected abstract Quantity quantityPerSecond();
}

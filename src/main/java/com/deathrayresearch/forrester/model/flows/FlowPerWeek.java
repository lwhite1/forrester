package com.deathrayresearch.forrester.model.flows;

import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.model.Flow;

import static com.deathrayresearch.forrester.measure.Units.WEEK;

/**
 * A rate specified as a quantity per week
 */
public abstract class FlowPerWeek extends Flow {

    public FlowPerWeek(String name) {
        super(name, WEEK);
    }

    @Override
    protected Quantity quantityPerTimeUnit() {
        return quantityPerWeek();
    }

    protected abstract Quantity quantityPerWeek();
}

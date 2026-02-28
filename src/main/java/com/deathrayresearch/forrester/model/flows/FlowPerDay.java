package com.deathrayresearch.forrester.model.flows;

import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.model.Flow;

import static com.deathrayresearch.forrester.measure.Units.DAY;

/**
 * A rate specified as a quantity per day (a.k.a. a daily rate)
 */
public abstract class FlowPerDay extends Flow {

    public FlowPerDay(String name) {
        super(name, DAY);
    }

    @Override
    protected Quantity quantityPerTimeUnit() {
        return quantityPerDay();
    }

    protected abstract Quantity quantityPerDay();
}

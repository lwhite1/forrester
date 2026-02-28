package com.deathrayresearch.forrester.model.flows;

import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.model.Flow;

import static com.deathrayresearch.forrester.measure.Units.YEAR;

/**
 * A rate specified as a quantity per year
 */
public abstract class FlowPerYear extends Flow {

    public FlowPerYear(String name) {
        super(name, YEAR);
    }

    @Override
    protected Quantity quantityPerTimeUnit() {
        return quantityPerYear();
    }

    protected abstract Quantity quantityPerYear();
}

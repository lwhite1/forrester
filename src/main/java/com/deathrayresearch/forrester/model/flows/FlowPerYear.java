package com.deathrayresearch.forrester.model.flows;

import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.TimeUnit;
import com.deathrayresearch.forrester.model.Flow;

import static com.deathrayresearch.forrester.measure.Units.YEAR;

/**
 *
 */
public abstract class FlowPerYear extends Flow {

    public FlowPerYear(String name) {
        super(name, YEAR);
    }

    private Quantity convert(Quantity quantity, TimeUnit newTimeUnit) {
        return RateConverter.convert(quantity, getTimeUnit(), newTimeUnit);
    }

    @Override
    public Quantity flowPerTimeUnit(TimeUnit timeUnit) {
        return convert(quantityPerYear(), timeUnit);
    }

    protected abstract Quantity quantityPerYear();

}

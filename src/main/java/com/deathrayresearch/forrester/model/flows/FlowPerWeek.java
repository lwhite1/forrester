package com.deathrayresearch.forrester.model.flows;

import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.TimeUnit;
import com.deathrayresearch.forrester.model.Flow;

import static com.deathrayresearch.forrester.measure.Units.WEEK;

/**
 *
 */
public abstract class FlowPerWeek extends Flow {

    public FlowPerWeek(String name) {
        super(name, WEEK);
    }

    private Quantity convert(Quantity quantity, TimeUnit newTimeUnit) {
        return RateConverter.convert(quantity, getTimeUnit(), newTimeUnit);
    }

    @Override
    public Quantity flowPerTimeUnit(TimeUnit timeUnit) {
        return convert(quantityPerWeek(), timeUnit);
    }

    protected abstract Quantity quantityPerWeek();

}

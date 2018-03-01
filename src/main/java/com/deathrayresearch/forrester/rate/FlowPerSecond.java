package com.deathrayresearch.forrester.rate;

import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.TimeUnit;

import static com.deathrayresearch.forrester.measure.Units.SECOND;

/**
 *
 */
public abstract class FlowPerSecond extends Flow {

    public FlowPerSecond(String name) {
        super(name, SECOND);
    }

    private Quantity convert(Quantity quantity, TimeUnit newTimeUnit) {
        return RateConverter.convert(quantity, getTimeUnit(), newTimeUnit);
    }

    @Override
    public Quantity flowPerTimeUnit(TimeUnit timeUnit) {
        return convert(quantityPerSecond(), timeUnit);
    }

    protected abstract Quantity quantityPerSecond();

}

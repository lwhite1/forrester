package com.deathrayresearch.forrester.rate;

import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.TimeUnit;

import static com.deathrayresearch.forrester.measure.Units.WEEK;

/**
 *
 */
public abstract class RatePerWeek extends AbstractRate {

    public RatePerWeek() {
        super(WEEK);
    }

    private Quantity convert(Quantity quantity, TimeUnit newTimeUnit) {
        return RateConverter.convert(quantity, getTimeUnit(), newTimeUnit);
    }

    @Override
    public String name() {
        return quantityPerWeek().getName();
    }

    @Override
    public Quantity flowPerTimeUnit(TimeUnit timeUnit) {
        return convert(quantityPerWeek(), timeUnit);
    }

    protected abstract Quantity quantityPerWeek();

}

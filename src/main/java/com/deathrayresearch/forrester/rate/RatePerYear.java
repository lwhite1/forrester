package com.deathrayresearch.forrester.rate;

import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.TimeUnit;
import com.deathrayresearch.forrester.measure.units.time.Times;

/**
 *
 */
public abstract class RatePerYear extends AbstractRate {

    public RatePerYear() {
        super(Times.YEAR);
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

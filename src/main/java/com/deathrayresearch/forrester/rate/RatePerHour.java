package com.deathrayresearch.forrester.rate;

import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.TimeUnit;
import com.deathrayresearch.forrester.measure.units.time.Times;

/**
 *
 */
public abstract class RatePerHour extends AbstractRate {

    public RatePerHour(String name) {
        super(name, Times.HOUR);
    }

    private Quantity convert(Quantity quantity, TimeUnit newTimeUnit) {
        return RateConverter.convert(quantity, getTimeUnit(), newTimeUnit);
    }

    @Override
    public Quantity flowPerTimeUnit(TimeUnit timeUnit) {
        return convert(quantityPerHour(), timeUnit);
    }

    protected abstract Quantity quantityPerHour();

}

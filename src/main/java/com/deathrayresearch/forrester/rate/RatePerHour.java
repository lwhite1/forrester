package com.deathrayresearch.forrester.rate;

import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.TimeUnit;

import static com.deathrayresearch.forrester.measure.Units.HOUR;

/**
 *
 */
public abstract class RatePerHour extends AbstractRate {

    public RatePerHour() {
        super(HOUR);
    }

    @Override
    public String name() {
        return quantityPerHour().getName();
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

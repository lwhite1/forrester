package com.deathrayresearch.forrester.rate;

import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.TimeUnit;

import static com.deathrayresearch.forrester.measure.Units.MINUTE;

/**
 *
 */
public abstract class RatePerMinute extends AbstractRate {

    public RatePerMinute() {
        super(MINUTE);
    }

    @Override
    public String name() {
        return quantityPerMinute().getName();
    }

    private Quantity convert(Quantity quantity, TimeUnit newTimeUnit) {
        return RateConverter.convert(quantity, getTimeUnit(), newTimeUnit);
    }

    @Override
    public Quantity flowPerTimeUnit(TimeUnit timeUnit) {
        return convert(quantityPerMinute(), timeUnit);
    }

    protected abstract Quantity quantityPerMinute();

}

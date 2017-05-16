package com.deathrayresearch.forrester.rate;

import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.TimeUnit;
import com.deathrayresearch.forrester.measure.units.time.Times;

/**
 * A rate specified as a quantity per day (a.k.a. a daily rate)
 */
public abstract class RatePerDay extends AbstractRate {

    public RatePerDay() {
        super(Times.DAY);
    }

    private Quantity convert(Quantity quantity, TimeUnit newTimeUnit) {
        return RateConverter.convert(quantity, Times.DAY, newTimeUnit);
    }

    @Override
    public Quantity flowPerTimeUnit(TimeUnit timeUnit) {
        return convert(quantityPerDay(), timeUnit);
    }

    protected abstract Quantity quantityPerDay();

}

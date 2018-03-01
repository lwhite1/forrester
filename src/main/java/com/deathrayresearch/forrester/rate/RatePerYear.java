package com.deathrayresearch.forrester.rate;

import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.TimeUnit;

import static com.deathrayresearch.forrester.measure.Units.YEAR;

/**
 *
 */
public abstract class RatePerYear extends AbstractRate {

    public RatePerYear() {
        super(YEAR);
    }

    private Quantity convert(Quantity quantity, TimeUnit newTimeUnit) {
        return RateConverter.convert(quantity, getTimeUnit(), newTimeUnit);
    }

    @Override
    public String name() {
        return quantityPerYear().getName();
    }

    @Override
    public Quantity flowPerTimeUnit(TimeUnit timeUnit) {
        return convert(quantityPerYear(), timeUnit);
    }

    protected abstract Quantity quantityPerYear();

}

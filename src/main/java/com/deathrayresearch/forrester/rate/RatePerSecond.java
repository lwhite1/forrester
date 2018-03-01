package com.deathrayresearch.forrester.rate;

import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.TimeUnit;

import static com.deathrayresearch.forrester.measure.Units.SECOND;

/**
 *
 */
public abstract class RatePerSecond extends AbstractRate {

    public RatePerSecond() {
        super(SECOND);
    }

    private Quantity convert(Quantity quantity, TimeUnit newTimeUnit) {
        return RateConverter.convert(quantity, getTimeUnit(), newTimeUnit);
    }

    @Override
    public String name() {
        return quantityPerSecond().getName();
    }

    @Override
    public Quantity flowPerTimeUnit(TimeUnit timeUnit) {
        return convert(quantityPerSecond(), timeUnit);
    }

    protected abstract Quantity quantityPerSecond();

}

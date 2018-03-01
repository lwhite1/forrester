package com.deathrayresearch.forrester.rate;

import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.TimeUnit;

import static com.deathrayresearch.forrester.measure.Units.HOUR;

/**
 *
 */
public abstract class FlowPerHour extends Flow {

    public FlowPerHour(String name) {
        super(name, HOUR);
    }

    @Override
    public String getName() {
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

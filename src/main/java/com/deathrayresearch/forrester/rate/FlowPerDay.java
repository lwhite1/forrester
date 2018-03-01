package com.deathrayresearch.forrester.rate;

import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.TimeUnit;
import com.google.common.base.Preconditions;

import static com.deathrayresearch.forrester.measure.Units.DAY;

/**
 * A rate specified as a quantity per day (a.k.a. a daily rate)
 */
public abstract class FlowPerDay extends Flow {

    public FlowPerDay(String name) {
        super(name, DAY);
    }

    private Quantity convert(Quantity quantity, TimeUnit newTimeUnit) {
        Preconditions.checkArgument(quantity!= null, "quantity is null in " + getName());
        Preconditions.checkArgument(newTimeUnit!= null, "newTimeUnit is null in " + getName());
        return RateConverter.convert(quantity, DAY, newTimeUnit);
    }

    @Override
    public Quantity flowPerTimeUnit(TimeUnit timeUnit) {
        Preconditions.checkArgument(timeUnit!= null, "timeUnit is null in " + getName());
        Quantity quantity = quantityPerDay();
        Preconditions.checkNotNull(quantity, "quantityPerDay() returned null in " + getName());
        return convert(quantity, timeUnit);
    }

    protected abstract Quantity quantityPerDay();
}

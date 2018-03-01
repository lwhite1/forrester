package com.deathrayresearch.forrester.rate;

import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.TimeUnit;

/**
 * Helps to create rates that are convertible to any time unit
 */
public abstract class Flow {

    private final TimeUnit timeUnit;

    private final String name;

    Flow(String name, TimeUnit unit) {
        this.name = name;
        this.timeUnit = unit;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    @Override
    public String toString() {
        return getName() + ": " + flowPerTimeUnit(timeUnit);
    }

    public String getName() {
        return name;
    }

    public abstract Quantity flowPerTimeUnit(TimeUnit timeUnit);

}

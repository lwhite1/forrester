package com.deathrayresearch.forrester.rate;

import com.deathrayresearch.forrester.measure.TimeUnit;

/**
 * Helps to create rates that are convertible to any time unit
 */
public abstract class AbstractRate implements Rate {

    private final String name;
    private final TimeUnit timeUnit;

    AbstractRate(String name, TimeUnit unit) {
        this.name = name;
        this.timeUnit = unit;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    @Override
    public String name() {
        return name;
    }
}

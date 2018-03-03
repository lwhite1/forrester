package com.deathrayresearch.forrester.model;

import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.TimeUnit;

import java.util.ArrayList;
import java.util.List;

/**
 * Helps to create rates that are convertible to any time unit
 */
public abstract class Flow {

    private final TimeUnit timeUnit;

    private final String name;

    private final List<Double> history = new ArrayList<>();

    public Flow(String name, TimeUnit unit) {
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

    public void recordValue(Quantity q) {
        history.add(q.getValue());
    }

    public double getHistoryAtTimeStep(int i) {
        if (i < 0 || history.size() <= i) {
            return 0;
        }
        return history.get(i);
    }
}

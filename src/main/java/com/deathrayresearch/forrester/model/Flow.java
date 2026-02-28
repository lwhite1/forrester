package com.deathrayresearch.forrester.model;

import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.TimeUnit;
import com.deathrayresearch.forrester.model.flows.RateConverter;

import java.util.ArrayList;
import java.util.List;

/**
 * Helps to create rates that are convertible to any time unit
 */
public abstract class Flow extends Element {

    private final TimeUnit timeUnit;

    private final List<Double> history = new ArrayList<>();
    private Stock source;
    private Stock sink;

    public Flow(String name, TimeUnit unit) {
        super(name);
        this.timeUnit = unit;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    @Override
    public String toString() {
        return getName() + ": " + flowPerTimeUnit(timeUnit);
    }

    public Quantity flowPerTimeUnit(TimeUnit timeUnit) {
        return RateConverter.convert(quantityPerTimeUnit(), this.timeUnit, timeUnit);
    }

    protected abstract Quantity quantityPerTimeUnit();

    public void recordValue(Quantity q) {
        history.add(q.getValue());
    }

    public double getHistoryAtTimeStep(int i) {
        if (i < 0 || history.size() <= i) {
            return 0;
        }
        return history.get(i);
    }

    public void setSource(Stock stock) {
        this.source = stock;
    }

    public void setSink(Stock stock) {
        this.sink = stock;
    }

    /**
     * Returns the stock that this flow drains from, or {@code null} if this flow has no source stock.
     */
    public Stock getSource() {
        return source;
    }

    /**
     * Returns the stock that this flow feeds into, or {@code null} if this flow has no sink stock.
     */
    public Stock getSink() {
        return sink;
    }
}

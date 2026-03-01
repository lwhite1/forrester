package com.deathrayresearch.forrester.model;

import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.TimeUnit;
import com.deathrayresearch.forrester.model.flows.RateConverter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Helps to create rates that are convertible to any time unit
 */
public abstract class Flow extends Element {

    private final TimeUnit timeUnit;

    private final List<Double> history = new ArrayList<>();
    private Stock source;
    private Stock sink;

    /**
     * Creates a new flow with the given name and native time unit.
     *
     * @param name the flow name
     * @param unit the time unit in which this flow's rate is expressed
     */
    public Flow(String name, TimeUnit unit) {
        super(name);
        this.timeUnit = unit;
    }

    /**
     * Creates a flow from a lambda that supplies the quantity per time unit, eliminating
     * the need for anonymous subclasses.
     *
     * @param name     the flow name
     * @param timeUnit the time unit in which this flow's rate is expressed
     * @param formula  a supplier that computes the quantity per time unit on each evaluation
     * @return a new flow that delegates to the given formula
     */
    public static Flow create(String name, TimeUnit timeUnit, Supplier<Quantity> formula) {
        return new Flow(name, timeUnit) {
            @Override
            protected Quantity quantityPerTimeUnit() {
                return formula.get();
            }
        };
    }

    /**
     * Returns the time unit in which this flow's rate is natively expressed.
     */
    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    @Override
    public String toString() {
        return getName() + ": " + flowPerTimeUnit(timeUnit);
    }

    /**
     * Returns this flow's rate converted to the given time unit.
     *
     * @param timeUnit the target time unit
     * @return the rate expressed per one unit of the given time unit
     */
    public Quantity flowPerTimeUnit(TimeUnit timeUnit) {
        return RateConverter.convert(quantityPerTimeUnit(), this.timeUnit, timeUnit);
    }

    protected abstract Quantity quantityPerTimeUnit();

    /**
     * Records the given quantity's value in this flow's history for the current time step.
     *
     * @param q the quantity to record
     */
    public void recordValue(Quantity q) {
        history.add(q.getValue());
    }

    /**
     * Returns the recorded flow value at the given time step index, or 0 if the index is out of range.
     *
     * @param i the zero-based time step index
     */
    public double getHistoryAtTimeStep(int i) {
        if (i < 0 || history.size() <= i) {
            return 0;
        }
        return history.get(i);
    }

    /**
     * Clears this flow's recorded history. Useful when re-running simulations.
     */
    public void clearHistory() {
        history.clear();
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

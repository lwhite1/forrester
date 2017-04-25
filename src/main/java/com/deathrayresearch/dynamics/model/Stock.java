package com.deathrayresearch.dynamics.model;

import com.deathrayresearch.dynamics.measure.Dimension;
import com.deathrayresearch.dynamics.measure.Quantity;
import com.deathrayresearch.dynamics.measure.Unit;

import java.util.HashSet;
import java.util.Set;

/**
 *
 */
public class Stock<E extends Dimension> extends Element implements Source<E>, Sink<E> {

    private Set<Flow<E>> inflows = new HashSet<>();
    private Set<Flow<E>> outflows = new HashSet<>();

    private Quantity<E> currentValue;

    public Stock(String name, Quantity<E> quantity) {
        super(name);
        this.currentValue = quantity;
    }

    public Stock(String name, double initialAmount, Unit<E> unit) {
        super(name);
        this.currentValue = new Quantity<>(initialAmount, unit);
    }

    public void addInflow(Flow<E> inFlow) {
        inflows.add(inFlow);
    }

    @Override
    public void addOutflow(Flow<E> outFlow) {
        outflows.add(outFlow);
    }

    public Set<Flow<E>> getInflows() {
        return inflows;
    }

    public Set<Flow<E>> getOutflows() {
        return outflows;
    }

    public Quantity<E> getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(Quantity<E> currentValue) {
        this.currentValue = currentValue;
    }
}

package com.deathrayresearch.forrester.model;

import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.Unit;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * A level or store of some quantity of interest
 */
public class Stock extends Element {

    private final Set<Flow> inflows = new HashSet<>();
    private final Set<Flow> outflows = new HashSet<>();

    private final Unit unit;
    private double value;

    public Stock(String name, double initialAmount, Unit unit) {
        super(name);
        this.unit = unit;
        this.value = initialAmount;
    }

    public void addInflow(Flow inFlow) {
        inflows.add(inFlow);
        inFlow.setSink(this);
    }

    public void addOutflow(Flow outFlow) {
        outflows.add(outFlow);
        outFlow.setSource(this);
    }

    public Set<Flow> getInflows() {
        return Collections.unmodifiableSet(inflows);
    }

    public Set<Flow> getOutflows() {
        return Collections.unmodifiableSet(outflows);
    }

    public Quantity getQuantity() {
        return new Quantity(value, unit);
    }

    public Unit getUnit() {
        return unit;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public double getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "Stock (" + getName() +"): " + getQuantity();
    }
}

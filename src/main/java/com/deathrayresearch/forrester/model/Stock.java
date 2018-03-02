package com.deathrayresearch.forrester.model;

import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.Unit;
import com.deathrayresearch.forrester.rate.Flow;

import java.util.HashSet;
import java.util.Set;

/**
 * A level or store of some quantity of interest
 */
public class Stock extends Element {

    private Set<Flow> inflows = new HashSet<>();
    private Set<Flow> outflows = new HashSet<>();

    private final String name;
    private final Quantity currentValue;

    public Stock(String name, double initialAmount, Unit unit) {
        this.name = name;
        this.currentValue = new Quantity(initialAmount, unit);
    }

    public void addInflow(Flow inFlow) {
        inflows.add(inFlow);
    }

    public void addOutflow(Flow outFlow) {
        outflows.add(outFlow);
    }

    public Set<Flow> getInflows() {
        return inflows;
    }

    public Set<Flow> getOutflows() {
        return outflows;
    }

    public Quantity getQuantity() {
        return currentValue;
    }

    public Unit getUnit() {
        return currentValue.getUnit();
    }

    public void setValue(double value) {
        this.getQuantity().setValue(value);
    }

    public double getValue() {
        return getQuantity().getValue();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "Stock (" + getName() +"): " + currentValue;
    }
}

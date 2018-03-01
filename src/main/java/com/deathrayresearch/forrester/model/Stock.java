package com.deathrayresearch.forrester.model;

import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.Unit;

import java.util.HashSet;
import java.util.Set;

/**
 * A level or store of some quantity of interest
 */
public class Stock extends Element {

    private Set<Flow> inflows = new HashSet<>();
    private Set<Flow> outflows = new HashSet<>();

    private Quantity currentValue;

    public Stock(Quantity quantity) {
        this.currentValue = quantity;
    }

    public Stock(String name, double initialAmount, Unit unit) {
        this.currentValue = new Quantity(name, initialAmount, unit);
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

    public double getCurrentValue() {
        return currentValue.getValue();
    }

    public Quantity getQuantity() {
        return currentValue;
    }

    public void setCurrentValue(Quantity currentValue) {
        this.currentValue = currentValue;
    }

    @Override
    public String getName() {
        return currentValue.getName();
    }

    @Override
    public String toString() {
        return "Stock (" + getName() +"): " + currentValue;
    }
}

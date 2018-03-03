package com.deathrayresearch.forrester.model;

import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.Unit;
import com.deathrayresearch.forrester.measure.units.time.Day;
import com.google.common.base.Preconditions;

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
        Preconditions.checkArgument(inFlow.flowPerTimeUnit(Day.getInstance()).isCompatibleWith(currentValue),
                "Inflow " + inFlow.getName() + " is incompatible with stock " + name + ". They're units" +
                        " have different dimensions. ");
        inflows.add(inFlow);
        inFlow.setSink(this);
    }

    public void addOutflow(Flow outFlow) {
        Preconditions.checkArgument(outFlow.flowPerTimeUnit(Day.getInstance()).isCompatibleWith(currentValue),
                "Outflow " + outFlow.getName() + " is incompatible with stock " + name + ". They're units" +
                        " have different dimensions. ");
        outflows.add(outFlow);
        outFlow.setSource(this);
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

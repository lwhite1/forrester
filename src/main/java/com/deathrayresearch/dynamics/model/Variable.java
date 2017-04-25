package com.deathrayresearch.dynamics.model;

/**
 *
 */
public class Variable extends Element {

    private double currentValue;

    public Variable(String name, double currentValue) {
        super(name);
        this.currentValue = currentValue;
    }

    public double getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(double currentValue) {
        this.currentValue = currentValue;
    }
}

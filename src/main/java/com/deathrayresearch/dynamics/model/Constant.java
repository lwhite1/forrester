package com.deathrayresearch.dynamics.model;

/**
 *  An unchanging exogenous value used in a model
 */
public class Constant extends Element {

    private final double currentValue;

    public Constant(String name, double currentValue) {
        super(name);
        this.currentValue = currentValue;
    }

    public double getCurrentValue() {
        return currentValue;
    }
}

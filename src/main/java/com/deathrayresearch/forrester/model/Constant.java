package com.deathrayresearch.forrester.model;

import com.deathrayresearch.forrester.measure.Unit;

/**
 *  An unchanging exogenous value used in a model
 */
public class Constant extends Element {

    private final double currentValue;
    private final Unit unit;

    public Constant(String name, Unit unit, double currentValue) {
        super(name);
        this.unit = unit;
        this.currentValue = currentValue;
    }

    public double getValue() {
        return currentValue;
    }

    public int getIntValue() {
        return Math.toIntExact(Math.round(currentValue));
    }

    public Unit getUnit() {
        return unit;
    }
}

package com.deathrayresearch.forrester.model;

import com.deathrayresearch.forrester.measure.Unit;

/**
 *  An unchanging exogenous value used in a model
 */
public class Constant extends Element {

    private final double currentValue;
    private final Unit unit;
    private final String name;

    public Constant(String name, Unit unit, double currentValue) {
        this.name = name;
        this.unit = unit;
        this.currentValue = currentValue;
    }

    @Override
    public String getName() {
        return name;
    }

    public double getValue() {
        return currentValue;
    }

    public Unit getUnit() {
        return unit;
    }
}

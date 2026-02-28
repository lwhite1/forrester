package com.deathrayresearch.forrester.model;

import com.deathrayresearch.forrester.measure.Unit;

/**
 *  An unchanging exogenous value used in a model
 */
public class Constant extends Element {

    private final double currentValue;
    private final Unit unit;

    /**
     * Creates a new constant with the given name, unit, and value.
     *
     * @param name         the constant name
     * @param unit         the unit of measure for this constant
     * @param currentValue the fixed value of this constant
     */
    public Constant(String name, Unit unit, double currentValue) {
        super(name);
        this.unit = unit;
        this.currentValue = currentValue;
    }

    /**
     * Returns the value of this constant.
     */
    public double getValue() {
        return currentValue;
    }

    /**
     * Returns the value of this constant rounded to the nearest integer.
     */
    public int getIntValue() {
        return Math.toIntExact(Math.round(currentValue));
    }

    public Unit getUnit() {
        return unit;
    }
}

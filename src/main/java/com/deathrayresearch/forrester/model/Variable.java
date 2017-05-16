package com.deathrayresearch.forrester.model;

import com.deathrayresearch.forrester.measure.Unit;

/**
 *  A variable is an object that returns its current value at any time as determined by the Formula.
 */
public class Variable extends Element {

    private final Formula formula;
    private final Unit unit;

    public Variable(String name, Unit unit, Formula formula) {
        super(name);
        this.formula = formula;
        this.unit = unit;
    }

    public double getCurrentValue() {
        return formula.getCurrentValue();
    }

    public Unit getUnit() {
        return unit;
    }

    public Formula getFormula() {
        return formula;
    }
}

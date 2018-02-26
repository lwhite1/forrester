package com.deathrayresearch.forrester.model;

import com.deathrayresearch.forrester.measure.Unit;

/**
 *  A variable is an object that returns its current value at any time as determined by the Formula.
 */
public class Variable extends Element {

    private final Formula formula;
    private final Unit unit;
    private final String name;

    public Variable(String name, Unit unit, Formula formula) {
        this.name = name;
        this.formula = formula;
        this.unit = unit;
    }

    @Override
    public String getName() {
        return name;
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

package com.deathrayresearch.dynamics.model;

/**
 *  A variable is an object that returns its current value at any time as determined by the Formula.
 */
public class Variable extends Element {

    private Formula formula;

    public Variable(String name, Formula formula) {
        super(name);
        this.formula = formula;
    }

    public double getCurrentValue() {
        return formula.getCurrentValue();
    }

}

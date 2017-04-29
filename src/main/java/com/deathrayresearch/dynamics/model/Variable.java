package com.deathrayresearch.dynamics.model;

/**
 *
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

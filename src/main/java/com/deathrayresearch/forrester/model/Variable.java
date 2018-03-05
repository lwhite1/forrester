package com.deathrayresearch.forrester.model;

import com.deathrayresearch.forrester.measure.Unit;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;

/**
 *  A variable is an object that returns its current value at any time as determined by the Formula.
 */
public class Variable extends Element {

    private final Formula formula;
    private final Unit unit;
    private final String name;

    private final DoubleArrayList history = new DoubleArrayList();

    public Variable(String name, Unit unit, Formula formula) {
        this.name = name;
        this.formula = formula;
        this.unit = unit;
    }

    @Override
    public String getName() {
        return name;
    }

    public double getValue() {
        return formula.getCurrentValue();
    }

    public Unit getUnit() {
        return unit;
    }

    public Formula getFormula() {
        return formula;
    }

    public double getHistoryAtTimeStep(int i) {
        if (i < 0 || history.size() <= i) {
            return 0;
        }
        return history.getDouble(i);
    }

    public void recordValue() {
        history.add(getValue());
    }
}

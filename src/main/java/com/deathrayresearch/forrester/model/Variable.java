package com.deathrayresearch.forrester.model;

import com.deathrayresearch.forrester.measure.Unit;
import java.util.ArrayList;
import java.util.List;

/**
 *  A variable is an object that returns its current value at any time as determined by the Formula.
 */
public class Variable extends Element {

    private final Formula formula;
    private final Unit unit;

    private final List<Double> history = new ArrayList<>();

    public Variable(String name, Unit unit, Formula formula) {
        super(name);
        this.formula = formula;
        this.unit = unit;
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
        return history.get(i);
    }

    public void recordValue() {
        history.add(getValue());
    }
}

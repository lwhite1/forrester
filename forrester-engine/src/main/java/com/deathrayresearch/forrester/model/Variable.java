package com.deathrayresearch.forrester.model;

import com.deathrayresearch.forrester.measure.Unit;
import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.List;

/**
 *  A variable is an object that returns its current value at any time as determined by the Formula.
 */
public class Variable extends Element {

    private final Formula formula;
    private final Unit unit;

    private final List<Double> history = new ArrayList<>();

    /**
     * Creates a new variable with the given name, unit, and formula.
     *
     * @param name    the variable name
     * @param unit    the unit of measure for this variable's value
     * @param formula the formula that computes this variable's current value
     */
    public Variable(String name, Unit unit, Formula formula) {
        super(name);
        Preconditions.checkNotNull(unit, "unit must not be null");
        Preconditions.checkNotNull(formula, "formula must not be null");
        this.formula = formula;
        this.unit = unit;
    }

    /**
     * Returns the current value of this variable as computed by its formula.
     */
    public double getValue() {
        return formula.getCurrentValue();
    }

    /**
     * Returns the unit of measure for this variable's value.
     */
    public Unit getUnit() {
        return unit;
    }

    /**
     * Returns the formula that computes this variable's value each timestep.
     */
    public Formula getFormula() {
        return formula;
    }

    /**
     * Returns the recorded value at the given time step index, or 0 if the index is out of range.
     *
     * @param i the zero-based time step index
     */
    public double getHistoryAtTimeStep(int i) {
        if (i < 0 || history.size() <= i) {
            return 0;
        }
        return history.get(i);
    }

    /**
     * Records the current value of this variable in its history for the current time step.
     */
    public void recordValue() {
        history.add(getValue());
    }

    /**
     * Clears this variable's recorded history. Useful when re-running simulations.
     */
    public void clearHistory() {
        history.clear();
    }
}

package systems.courant.sd.model;

import com.carrotsearch.hppc.DoubleArrayList;
import systems.courant.sd.measure.Unit;
import com.google.common.base.Preconditions;

/**
 *  A variable is an object that returns its current value at any time as determined by the Formula.
 */
public class Variable extends Element {

    private final Formula formula;
    private final Unit unit;

    private final DoubleArrayList history = new DoubleArrayList();

    /**
     * Cached value from the most recent evaluation, used as the initial guess
     * when breaking algebraic loops via the re-entrancy guard.
     */
    private double cachedValue;

    /**
     * True while this variable's formula is being evaluated. A re-entrant call
     * (caused by an algebraic loop) returns {@link #cachedValue} instead of
     * recursing, effectively using the previous timestep's value to break the cycle.
     */
    private boolean evaluating;

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
     *
     * <p>If this method is called re-entrantly (because of an algebraic loop in the
     * dependency graph), the cached value from the previous evaluation is returned
     * instead of recursing infinitely. This is equivalent to Vensim's approach of
     * using the previous timestep value to break algebraic loops.
     */
    public double getValue() {
        if (evaluating) {
            return cachedValue;
        }
        evaluating = true;
        try {
            cachedValue = formula.getCurrentValue();
            return cachedValue;
        } finally {
            evaluating = false;
        }
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
    public double getHistoryAtTimeStep(long i) {
        if (i < 0 || i >= history.size()) {
            return 0;
        }
        return history.get((int) i);
    }

    /**
     * Records the current value of this variable in its history for the current time step.
     */
    public void recordValue() {
        history.add(getValue());
    }

    /**
     * Clears this variable's recorded history and resets the cached value.
     * Useful when re-running simulations.
     */
    public void clearHistory() {
        history.clear();
        cachedValue = 0;
    }
}

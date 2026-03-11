package systems.courant.shrewd.model;

import systems.courant.shrewd.measure.Unit;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.IntToDoubleFunction;

/**
 * An arrayed variable that wraps N plain {@link Variable} instances — one per element of a
 * {@link Subscript} dimension.
 *
 * <p>Each underlying variable is named {@code "baseName[label]"} and evaluates an index-aware
 * formula to compute its current value.
 */
public class ArrayedVariable {

    private final String baseName;
    private final Subscript subscript;
    private final Variable[] variables;

    private ArrayedVariable(String baseName, Subscript subscript, Variable[] variables) {
        this.baseName = baseName;
        this.subscript = subscript;
        this.variables = variables;
    }

    /**
     * Creates an arrayed variable with an index-aware formula for each element.
     *
     * @param baseName  the base name (each variable is named "baseName[label]")
     * @param unit      the unit of measure
     * @param subscript the subscript dimension
     * @param formula   a function from element index to the variable's current value
     * @return a new arrayed variable
     */
    public static ArrayedVariable create(String baseName, Unit unit, Subscript subscript,
                                         IntToDoubleFunction formula) {
        Variable[] variables = new Variable[subscript.size()];
        for (int i = 0; i < subscript.size(); i++) {
            final int index = i;
            variables[i] = new Variable(
                    baseName + "[" + subscript.getLabel(i) + "]",
                    unit,
                    () -> formula.applyAsDouble(index)
            );
        }
        return new ArrayedVariable(baseName, subscript, variables);
    }

    /**
     * Returns the underlying variable at the given index.
     */
    public Variable getVariable(int index) {
        return variables[index];
    }

    /**
     * Returns the underlying variable with the given label.
     */
    public Variable getVariable(String label) {
        return variables[subscript.indexOf(label)];
    }

    /**
     * Returns all underlying variables as an unmodifiable list.
     */
    public List<Variable> getVariables() {
        return Collections.unmodifiableList(Arrays.asList(variables));
    }

    /**
     * Returns the current value of the element at the given index.
     */
    public double getValue(int index) {
        return variables[index].getValue();
    }

    /**
     * Returns the current value of the element with the given label.
     */
    public double getValue(String label) {
        return variables[subscript.indexOf(label)].getValue();
    }

    /**
     * Returns the sum of all element values.
     */
    public double sum() {
        double total = 0;
        for (Variable variable : variables) {
            total += variable.getValue();
        }
        return total;
    }

    /**
     * Returns the number of elements.
     */
    public int size() {
        return variables.length;
    }

    /**
     * Returns a snapshot of the current variable values as an {@link IndexedValue}.
     */
    public IndexedValue getIndexedValue() {
        double[] vals = new double[variables.length];
        for (int i = 0; i < variables.length; i++) {
            vals[i] = variables[i].getValue();
        }
        return IndexedValue.of(subscript, vals);
    }

    /**
     * Returns the base name shared by all underlying variables.
     */
    public String getBaseName() {
        return baseName;
    }

    /**
     * Returns the subscript dimension used by this arrayed variable.
     */
    public Subscript getSubscript() {
        return subscript;
    }

    @Override
    public String toString() {
        return "ArrayedVariable(" + baseName + ", " + subscript + ")";
    }
}

package com.deathrayresearch.forrester.model;

import com.deathrayresearch.forrester.measure.Unit;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.IntToDoubleFunction;
import java.util.function.ToDoubleFunction;

/**
 * A multi-dimensional arrayed variable that wraps N×M×... plain {@link Variable} instances — one per
 * combination of labels across all {@link Subscript} dimensions in a {@link SubscriptRange}.
 *
 * <p>Each underlying variable is named using comma-separated labels, e.g.,
 * {@code "Density[North,Young]"}, and evaluates a formula to compute its current value.
 */
public class MultiArrayedVariable {

    private final String baseName;
    private final SubscriptRange range;
    private final Variable[] variables;

    private MultiArrayedVariable(String baseName, SubscriptRange range, Variable[] variables) {
        this.baseName = baseName;
        this.range = range;
        this.variables = variables;
    }

    /**
     * Creates a multi-arrayed variable with a coordinate-aware formula for each element.
     *
     * @param baseName the base name (each variable is named "baseName[label0,label1,...]")
     * @param unit     the unit of measure
     * @param range    the multi-dimensional subscript range
     * @param formula  a function from coordinate array to the variable's current value
     * @return a new multi-arrayed variable
     */
    public static MultiArrayedVariable create(String baseName, Unit unit, SubscriptRange range,
                                               ToDoubleFunction<int[]> formula) {
        Variable[] variables = new Variable[range.totalSize()];
        for (int i = 0; i < range.totalSize(); i++) {
            final int[] coords = range.toCoordinates(i);
            variables[i] = new Variable(
                    range.composeName(baseName, i),
                    unit,
                    () -> formula.applyAsDouble(coords)
            );
        }
        return new MultiArrayedVariable(baseName, range, variables);
    }

    /**
     * Creates a multi-arrayed variable with a flat-index formula for each element.
     *
     * @param baseName the base name
     * @param unit     the unit of measure
     * @param range    the multi-dimensional subscript range
     * @param formula  a function from flat index to the variable's current value
     * @return a new multi-arrayed variable
     */
    public static MultiArrayedVariable createByIndex(String baseName, Unit unit, SubscriptRange range,
                                                      IntToDoubleFunction formula) {
        Variable[] variables = new Variable[range.totalSize()];
        for (int i = 0; i < range.totalSize(); i++) {
            final int index = i;
            variables[i] = new Variable(
                    range.composeName(baseName, i),
                    unit,
                    () -> formula.applyAsDouble(index)
            );
        }
        return new MultiArrayedVariable(baseName, range, variables);
    }

    /**
     * Returns the underlying variable at the given flat index.
     */
    public Variable getVariable(int flatIndex) {
        return variables[flatIndex];
    }

    /**
     * Returns the underlying variable at the given coordinates.
     */
    public Variable getVariableAt(int... coords) {
        return variables[range.toFlatIndex(coords)];
    }

    /**
     * Returns the underlying variable at the given labels.
     */
    public Variable getVariableAt(String... labels) {
        return variables[range.toFlatIndex(labels)];
    }

    /**
     * Returns all underlying variables as an unmodifiable list.
     */
    public List<Variable> getVariables() {
        return Collections.unmodifiableList(Arrays.asList(variables));
    }

    /**
     * Returns the current value of the element at the given flat index.
     */
    public double getValue(int flatIndex) {
        return variables[flatIndex].getValue();
    }

    /**
     * Returns the current value of the element at the given coordinates.
     */
    public double getValueAt(int... coords) {
        return variables[range.toFlatIndex(coords)].getValue();
    }

    /**
     * Returns the current value of the element at the given labels.
     */
    public double getValueAt(String... labels) {
        return variables[range.toFlatIndex(labels)].getValue();
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
     * Collapses one dimension by summing over it, returning an array of doubles
     * sized to the product of the remaining dimensions.
     *
     * @param dimensionIndex the index of the dimension to collapse
     * @return an array of sums, one per combination of the remaining dimensions
     */
    public double[] sumOver(int dimensionIndex) {
        int collapsedSize = range.getSubscript(dimensionIndex).size();
        int resultSize = range.totalSize() / collapsedSize;
        double[] result = new double[resultSize];

        SubscriptRange reducedRange = range.removeDimension(dimensionIndex);

        for (int flatIdx = 0; flatIdx < range.totalSize(); flatIdx++) {
            int[] coords = range.toCoordinates(flatIdx);
            int[] reducedCoords = new int[coords.length - 1];
            int j = 0;
            for (int d = 0; d < coords.length; d++) {
                if (d != dimensionIndex) {
                    reducedCoords[j++] = coords[d];
                }
            }
            int reducedFlat = reducedRange.toFlatIndex(reducedCoords);
            result[reducedFlat] += variables[flatIdx].getValue();
        }
        return result;
    }

    /**
     * Returns the number of elements.
     */
    public int size() {
        return variables.length;
    }

    public String getBaseName() {
        return baseName;
    }

    public SubscriptRange getRange() {
        return range;
    }

    @Override
    public String toString() {
        return "MultiArrayedVariable(" + baseName + ", " + range + ")";
    }
}

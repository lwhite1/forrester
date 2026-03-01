package com.deathrayresearch.forrester.model;

import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

/**
 * A multi-dimensional index manager that composes one or more {@link Subscript} objects into
 * a single range spanning all combinations.
 *
 * <p>For example, composing {@code Region[North, South]} and {@code AgeGroup[Young, Elder]}
 * yields a range of size 4 with combinations:
 * {@code [North,Young], [North,Elder], [South,Young], [South,Elder]} in row-major order.
 *
 * <p>Row-major convention: for subscripts with sizes {@code [d0, d1, ...]}, strides are
 * {@code [d1*d2*..., d2*d3*..., ..., 1]} and flat index = {@code i0*stride0 + i1*stride1 + ...}.
 */
public class SubscriptRange {

    private final List<Subscript> subscripts;
    private final int totalSize;
    private final int[] strides;
    private final List<List<String>> combinations;

    /**
     * Creates a multi-dimensional range from the given subscript dimensions.
     *
     * @param subscripts the subscript dimensions (must contain at least one; no duplicate dimension names)
     * @throws IllegalArgumentException if the list is empty or contains duplicate dimension names
     */
    public SubscriptRange(List<Subscript> subscripts) {
        if (subscripts == null || subscripts.isEmpty()) {
            throw new IllegalArgumentException("SubscriptRange requires at least one subscript");
        }
        Set<String> names = new HashSet<>();
        for (Subscript s : subscripts) {
            if (!names.add(s.getName())) {
                throw new IllegalArgumentException("Duplicate subscript dimension name: " + s.getName());
            }
        }
        this.subscripts = Collections.unmodifiableList(new ArrayList<>(subscripts));

        // Compute strides (row-major), using Math.multiplyExact to detect overflow
        int dims = subscripts.size();
        this.strides = new int[dims];
        int stride = 1;
        for (int d = dims - 1; d >= 0; d--) {
            strides[d] = stride;
            try {
                stride = Math.multiplyExact(stride, subscripts.get(d).size());
            } catch (ArithmeticException e) {
                throw new IllegalArgumentException(
                        "SubscriptRange total size overflows int: product of dimension sizes is too large");
            }
        }
        this.totalSize = stride;

        // Compute cartesian product of all label lists
        List<List<String>> labelLists = new ArrayList<>(dims);
        for (Subscript s : subscripts) {
            labelLists.add(s.getLabels());
        }
        List<List<String>> product = Lists.cartesianProduct(labelLists);
        // Materialize into an immutable list
        List<List<String>> materialized = new ArrayList<>(product.size());
        for (List<String> combo : product) {
            materialized.add(Collections.unmodifiableList(new ArrayList<>(combo)));
        }
        this.combinations = Collections.unmodifiableList(materialized);
    }

    /**
     * Returns the total number of elements (product of all dimension sizes).
     */
    public int totalSize() {
        return totalSize;
    }

    /**
     * Returns the number of dimensions.
     */
    public int dimensionCount() {
        return subscripts.size();
    }

    /**
     * Returns the subscript at the given dimension index.
     */
    public Subscript getSubscript(int dim) {
        return subscripts.get(dim);
    }

    /**
     * Returns an unmodifiable list of all subscript dimensions.
     */
    public List<Subscript> getSubscripts() {
        return subscripts;
    }

    /**
     * Converts coordinate indices to a flat (row-major) index.
     *
     * @param coords one index per dimension
     * @return the flat index
     * @throws IllegalArgumentException if the number of coordinates doesn't match the dimension count
     *                                  or any coordinate is out of range
     */
    public int toFlatIndex(int... coords) {
        if (coords.length != subscripts.size()) {
            throw new IllegalArgumentException(
                    "Expected " + subscripts.size() + " coordinates but got " + coords.length);
        }
        int flat = 0;
        for (int d = 0; d < coords.length; d++) {
            int size = subscripts.get(d).size();
            if (coords[d] < 0 || coords[d] >= size) {
                throw new IllegalArgumentException(
                        "Coordinate " + coords[d] + " out of range for dimension '"
                                + subscripts.get(d).getName() + "' (size " + size + ")");
            }
            flat += coords[d] * strides[d];
        }
        return flat;
    }

    /**
     * Converts label names to a flat (row-major) index.
     *
     * @param labels one label per dimension
     * @return the flat index
     * @throws IllegalArgumentException if the number of labels doesn't match the dimension count
     *                                  or any label is not found
     */
    public int toFlatIndex(String... labels) {
        if (labels.length != subscripts.size()) {
            throw new IllegalArgumentException(
                    "Expected " + subscripts.size() + " labels but got " + labels.length);
        }
        int[] coords = new int[labels.length];
        for (int d = 0; d < labels.length; d++) {
            coords[d] = subscripts.get(d).indexOf(labels[d]);
        }
        return toFlatIndex(coords);
    }

    /**
     * Converts a flat index to coordinate indices.
     *
     * @param flatIndex the flat index (0-based, must be &lt; totalSize)
     * @return an array of coordinate indices, one per dimension
     * @throws IllegalArgumentException if the flat index is out of range
     */
    public int[] toCoordinates(int flatIndex) {
        if (flatIndex < 0 || flatIndex >= totalSize) {
            throw new IllegalArgumentException(
                    "Flat index " + flatIndex + " out of range (totalSize=" + totalSize + ")");
        }
        int[] coords = new int[subscripts.size()];
        int remaining = flatIndex;
        for (int d = 0; d < coords.length; d++) {
            coords[d] = remaining / strides[d];
            remaining %= strides[d];
        }
        return coords;
    }

    /**
     * Composes a name for the element at the given flat index.
     * For example, {@code composeName("Pop", 0)} might return {@code "Pop[North,Young]"}.
     *
     * @param baseName  the base element name
     * @param flatIndex the flat index
     * @return the composed name
     */
    public String composeName(String baseName, int flatIndex) {
        if (flatIndex < 0 || flatIndex >= totalSize) {
            throw new IllegalArgumentException(
                    "Flat index " + flatIndex + " out of range (totalSize=" + totalSize + ")");
        }
        List<String> labels = combinations.get(flatIndex);
        StringJoiner joiner = new StringJoiner(",");
        for (String label : labels) {
            joiner.add(label);
        }
        return baseName + "[" + joiner + "]";
    }

    /**
     * Returns the labels at the given flat index.
     *
     * @param flatIndex the flat index
     * @return an unmodifiable list of labels, one per dimension
     */
    public List<String> getLabelsAt(int flatIndex) {
        if (flatIndex < 0 || flatIndex >= totalSize) {
            throw new IllegalArgumentException(
                    "Flat index " + flatIndex + " out of range (totalSize=" + totalSize + ")");
        }
        return combinations.get(flatIndex);
    }

    /**
     * Returns all label combinations in row-major order.
     *
     * @return an unmodifiable list of unmodifiable label lists
     */
    public List<List<String>> allCombinations() {
        return combinations;
    }

    /**
     * Returns a new SubscriptRange with the specified dimension removed.
     *
     * @param dimIndex the index of the dimension to remove
     * @return a new SubscriptRange with one fewer dimension
     * @throws IllegalArgumentException if dimIndex is out of range or would result in an empty range
     */
    public SubscriptRange removeDimension(int dimIndex) {
        if (dimIndex < 0 || dimIndex >= subscripts.size()) {
            throw new IllegalArgumentException(
                    "Dimension index " + dimIndex + " out of range (dimensionCount=" + subscripts.size() + ")");
        }
        if (subscripts.size() == 1) {
            throw new IllegalArgumentException("Cannot remove the only dimension from a SubscriptRange");
        }
        List<Subscript> remaining = new ArrayList<>(subscripts.size() - 1);
        for (int d = 0; d < subscripts.size(); d++) {
            if (d != dimIndex) {
                remaining.add(subscripts.get(d));
            }
        }
        return new SubscriptRange(remaining);
    }

    @Override
    public String toString() {
        StringJoiner joiner = new StringJoiner(" × ");
        for (Subscript s : subscripts) {
            joiner.add(s.toString());
        }
        return "SubscriptRange(" + joiner + ", size=" + totalSize + ")";
    }
}

package com.deathrayresearch.forrester.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * An immutable multi-dimensional value with named dimensions that supports automatic
 * broadcasting in arithmetic operations.
 *
 * <p>When two {@code IndexedValue} instances with different dimensions are combined
 * (e.g., added or multiplied), shared dimensions are aligned by name and non-shared
 * dimensions are expanded via outer product — matching the "intelligent array" semantics
 * of tools like Analytica.
 *
 * <p>Examples:
 * <ul>
 *   <li>{@code [Region] + [Region]} → elementwise {@code [Region]}</li>
 *   <li>{@code scalar + [Region]} → broadcast scalar to every element → {@code [Region]}</li>
 *   <li>{@code [Region] * [AgeGroup]} → outer product → {@code [Region × AgeGroup]}</li>
 *   <li>{@code [Region × AgeGroup] * [Region]} → broadcast Region-only value across AgeGroup
 *       → {@code [Region × AgeGroup]}</li>
 * </ul>
 *
 * <p>A scalar {@code IndexedValue} has a null range and a single-element value array.
 */
public final class IndexedValue {

    private final SubscriptRange range; // null for scalar
    private final double[] values;

    private IndexedValue(SubscriptRange range, double[] values) {
        this.range = range;
        this.values = values;
    }

    // --- Factory methods ---

    /**
     * Creates a scalar (dimensionless) indexed value.
     */
    public static IndexedValue scalar(double value) {
        return new IndexedValue(null, new double[]{value});
    }

    /**
     * Creates a one-dimensional indexed value from a single subscript.
     *
     * @param subscript the dimension
     * @param values    one value per subscript label
     * @throws IllegalArgumentException if the array length doesn't match the subscript size
     */
    public static IndexedValue of(Subscript subscript, double... values) {
        SubscriptRange range = new SubscriptRange(List.of(subscript));
        if (values.length != range.totalSize()) {
            throw new IllegalArgumentException(
                    "Expected " + range.totalSize() + " values but got " + values.length);
        }
        return new IndexedValue(range, values.clone());
    }

    /**
     * Creates a multi-dimensional indexed value from a subscript range.
     *
     * @param range  the multi-dimensional range
     * @param values one value per element in row-major order
     * @throws IllegalArgumentException if the array length doesn't match the range's total size
     */
    public static IndexedValue of(SubscriptRange range, double[] values) {
        if (values.length != range.totalSize()) {
            throw new IllegalArgumentException(
                    "Expected " + range.totalSize() + " values but got " + values.length);
        }
        return new IndexedValue(range, values.clone());
    }

    /**
     * Creates an indexed value with every element set to the same value.
     */
    public static IndexedValue fill(SubscriptRange range, double value) {
        double[] values = new double[range.totalSize()];
        Arrays.fill(values, value);
        return new IndexedValue(range, values);
    }

    /**
     * Creates an indexed value with every element set to the same value, for a single subscript.
     */
    public static IndexedValue fill(Subscript subscript, double value) {
        return fill(new SubscriptRange(List.of(subscript)), value);
    }

    // --- Accessors ---

    /**
     * Returns {@code true} if this value has no dimensions (is a scalar).
     */
    public boolean isScalar() {
        return range == null;
    }

    /**
     * Returns the subscript range, or {@code null} if this is a scalar.
     */
    public SubscriptRange getRange() {
        return range;
    }

    /**
     * Returns the number of elements (1 for scalars).
     */
    public int size() {
        return values.length;
    }

    /**
     * Returns the value at the given flat index.
     */
    public double get(int flatIndex) {
        return values[flatIndex];
    }

    /**
     * Returns the value at the given coordinates.
     *
     * @throws IllegalStateException if this is a scalar
     */
    public double getAt(int... coords) {
        if (range == null) {
            throw new IllegalStateException("Cannot use coordinate access on a scalar");
        }
        return values[range.toFlatIndex(coords)];
    }

    /**
     * Returns the value at the given labels.
     *
     * @throws IllegalStateException if this is a scalar
     */
    public double getAt(String... labels) {
        if (range == null) {
            throw new IllegalStateException("Cannot use label access on a scalar");
        }
        return values[range.toFlatIndex(labels)];
    }

    /**
     * Returns the scalar value. Throws if this is not a scalar.
     */
    public double scalarValue() {
        if (!isScalar()) {
            throw new IllegalStateException("Not a scalar; use get() or getAt() with coordinates");
        }
        return values[0];
    }

    /**
     * Returns a copy of the underlying values array.
     */
    public double[] toArray() {
        return values.clone();
    }

    // --- Arithmetic with broadcasting ---

    public IndexedValue add(IndexedValue other) {
        return binaryOp(other, Double::sum);
    }

    public IndexedValue subtract(IndexedValue other) {
        return binaryOp(other, (a, b) -> a - b);
    }

    public IndexedValue multiply(IndexedValue other) {
        return binaryOp(other, (a, b) -> a * b);
    }

    public IndexedValue divide(IndexedValue other) {
        return binaryOp(other, (a, b) -> {
            if (b == 0) {
                throw new ArithmeticException("Division by zero in IndexedValue");
            }
            return a / b;
        });
    }

    // --- Scalar convenience overloads ---

    public IndexedValue add(double scalar) {
        return add(scalar(scalar));
    }

    public IndexedValue subtract(double scalar) {
        return subtract(scalar(scalar));
    }

    public IndexedValue multiply(double scalar) {
        return multiply(scalar(scalar));
    }

    public IndexedValue divide(double scalar) {
        return divide(scalar(scalar));
    }

    /**
     * Returns a new IndexedValue with every element negated.
     */
    public IndexedValue negate() {
        double[] result = new double[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = -values[i];
        }
        return new IndexedValue(range, result);
    }

    // --- Aggregation ---

    /**
     * Returns the sum of all elements.
     */
    public double sum() {
        double total = 0;
        for (double v : values) {
            total += v;
        }
        return total;
    }

    /**
     * Returns the arithmetic mean of all elements.
     */
    public double mean() {
        return sum() / values.length;
    }

    /**
     * Returns the maximum element value.
     */
    public double max() {
        double max = Double.NEGATIVE_INFINITY;
        for (double v : values) {
            if (v > max) {
                max = v;
            }
        }
        return max;
    }

    /**
     * Returns the minimum element value.
     */
    public double min() {
        double min = Double.POSITIVE_INFINITY;
        for (double v : values) {
            if (v < min) {
                min = v;
            }
        }
        return min;
    }

    /**
     * Collapses the given dimension by summing over it.
     *
     * <p>If this value has only one dimension, the result is a scalar.
     * Otherwise, the result has one fewer dimension.
     *
     * @param dimension the subscript dimension to collapse
     * @return a new IndexedValue with the dimension removed
     * @throws IllegalStateException    if this is a scalar
     * @throws IllegalArgumentException if the dimension is not found
     */
    public IndexedValue sumOver(Subscript dimension) {
        if (range == null) {
            throw new IllegalStateException("Cannot sumOver on a scalar");
        }
        int dimIndex = findDimension(range, dimension);
        if (dimIndex < 0) {
            throw new IllegalArgumentException(
                    "Dimension '" + dimension.getName() + "' not found in this value's range");
        }
        if (range.dimensionCount() == 1) {
            return scalar(sum());
        }
        SubscriptRange reducedRange = range.removeDimension(dimIndex);
        double[] result = new double[reducedRange.totalSize()];
        for (int flatIdx = 0; flatIdx < range.totalSize(); flatIdx++) {
            int[] coords = range.toCoordinates(flatIdx);
            int[] reducedCoords = new int[coords.length - 1];
            int j = 0;
            for (int d = 0; d < coords.length; d++) {
                if (d != dimIndex) {
                    reducedCoords[j++] = coords[d];
                }
            }
            int reducedFlat = reducedRange.toFlatIndex(reducedCoords);
            result[reducedFlat] += values[flatIdx];
        }
        return new IndexedValue(reducedRange, result);
    }

    // --- Broadcasting internals ---

    @FunctionalInterface
    private interface DoubleBinaryOp {
        double apply(double a, double b);
    }

    private IndexedValue binaryOp(IndexedValue other, DoubleBinaryOp op) {
        // Both scalar
        if (this.isScalar() && other.isScalar()) {
            return scalar(op.apply(this.values[0], other.values[0]));
        }
        // One scalar, one indexed
        if (this.isScalar()) {
            double[] result = new double[other.values.length];
            for (int i = 0; i < result.length; i++) {
                result[i] = op.apply(this.values[0], other.values[i]);
            }
            return new IndexedValue(other.range, result);
        }
        if (other.isScalar()) {
            double[] result = new double[this.values.length];
            for (int i = 0; i < result.length; i++) {
                result[i] = op.apply(this.values[i], other.values[0]);
            }
            return new IndexedValue(this.range, result);
        }
        // Both indexed — need broadcasting
        return broadcastOp(other, op);
    }

    private IndexedValue broadcastOp(IndexedValue other, DoubleBinaryOp op) {
        List<Subscript> leftDims = this.range.getSubscripts();
        List<Subscript> rightDims = other.range.getSubscripts();

        // Validate: same-named dimensions must have identical labels
        for (Subscript ls : leftDims) {
            for (Subscript rs : rightDims) {
                if (ls.getName().equals(rs.getName()) && !ls.getLabels().equals(rs.getLabels())) {
                    throw new IllegalArgumentException(
                            "Dimension '" + ls.getName()
                                    + "' has different labels in left and right operands");
                }
            }
        }

        // Fast path: identical ranges → elementwise
        if (rangesEqual(this.range, other.range)) {
            double[] result = new double[this.values.length];
            for (int i = 0; i < result.length; i++) {
                result[i] = op.apply(this.values[i], other.values[i]);
            }
            return new IndexedValue(this.range, result);
        }

        // Build result dimensions: all left dims, then right-only dims
        List<Subscript> resultDims = new ArrayList<>(leftDims);
        for (Subscript rs : rightDims) {
            if (findDimension(this.range, rs) < 0) {
                resultDims.add(rs);
            }
        }
        SubscriptRange resultRange = new SubscriptRange(resultDims);

        // Build mappings from result dimension index to left/right dimension index
        // -1 means the dimension is absent in that operand (broadcast)
        int[] leftDimMap = new int[resultDims.size()];
        int[] rightDimMap = new int[resultDims.size()];
        Arrays.fill(leftDimMap, -1);
        Arrays.fill(rightDimMap, -1);

        for (int rd = 0; rd < resultDims.size(); rd++) {
            String name = resultDims.get(rd).getName();
            leftDimMap[rd] = findDimensionByName(leftDims, name);
            rightDimMap[rd] = findDimensionByName(rightDims, name);
        }

        double[] result = new double[resultRange.totalSize()];
        int[] leftCoords = new int[leftDims.size()];
        int[] rightCoords = new int[rightDims.size()];

        for (int i = 0; i < resultRange.totalSize(); i++) {
            int[] resultCoords = resultRange.toCoordinates(i);

            // Map result coordinates to left operand coordinates
            for (int rd = 0; rd < resultDims.size(); rd++) {
                if (leftDimMap[rd] >= 0) {
                    leftCoords[leftDimMap[rd]] = resultCoords[rd];
                }
            }

            // Map result coordinates to right operand coordinates
            for (int rd = 0; rd < resultDims.size(); rd++) {
                if (rightDimMap[rd] >= 0) {
                    rightCoords[rightDimMap[rd]] = resultCoords[rd];
                }
            }

            int leftFlat = this.range.toFlatIndex(leftCoords);
            int rightFlat = other.range.toFlatIndex(rightCoords);
            result[i] = op.apply(this.values[leftFlat], other.values[rightFlat]);
        }

        return new IndexedValue(resultRange, result);
    }

    // --- Helpers ---

    private static int findDimension(SubscriptRange range, Subscript dimension) {
        for (int d = 0; d < range.dimensionCount(); d++) {
            if (range.getSubscript(d).getName().equals(dimension.getName())) {
                return d;
            }
        }
        return -1;
    }

    private static int findDimensionByName(List<Subscript> dims, String name) {
        for (int d = 0; d < dims.size(); d++) {
            if (dims.get(d).getName().equals(name)) {
                return d;
            }
        }
        return -1;
    }

    private static boolean rangesEqual(SubscriptRange a, SubscriptRange b) {
        if (a.dimensionCount() != b.dimensionCount()) {
            return false;
        }
        for (int d = 0; d < a.dimensionCount(); d++) {
            Subscript sa = a.getSubscript(d);
            Subscript sb = b.getSubscript(d);
            if (!sa.getName().equals(sb.getName())) {
                return false;
            }
            if (!sa.getLabels().equals(sb.getLabels())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        if (isScalar()) {
            return "IndexedValue(scalar=" + values[0] + ")";
        }
        return "IndexedValue(" + range + ", values=" + Arrays.toString(values) + ")";
    }
}

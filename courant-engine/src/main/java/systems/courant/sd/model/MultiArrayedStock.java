package systems.courant.sd.model;

import systems.courant.sd.measure.Unit;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A multi-dimensional arrayed stock that wraps N×M×... plain {@link Stock} instances — one per
 * combination of labels across all {@link Subscript} dimensions in a {@link SubscriptRange}.
 *
 * <p>Each underlying stock is named using comma-separated labels, e.g., {@code "Population[North,Young]"}.
 * When added to a {@link Model} via {@link Model#addMultiArrayedStock(MultiArrayedStock)}, the
 * individual stocks are expanded into the model's flat stock list, so the simulation loop and all
 * output infrastructure work without changes.
 */
public class MultiArrayedStock {

    private final String baseName;
    private final SubscriptRange range;
    private final Stock[] stocks;
    private final Unit unit;

    /**
     * Creates a multi-arrayed stock with the same initial value for every element.
     * Uses {@link NegativeValuePolicy#ALLOW} by default.
     *
     * @param baseName     the base name (each stock is named "baseName[label0,label1,...]")
     * @param range        the multi-dimensional subscript range
     * @param initialValue the initial value for every element
     * @param unit         the unit of measure
     */
    public MultiArrayedStock(String baseName, SubscriptRange range, double initialValue, Unit unit) {
        this(baseName, range, initialValue, unit, NegativeValuePolicy.ALLOW);
    }

    /**
     * Creates a multi-arrayed stock with the same initial value for every element and
     * an explicit negative-value policy.
     *
     * @param baseName            the base name
     * @param range               the multi-dimensional subscript range
     * @param initialValue        the initial value for every element
     * @param unit                the unit of measure
     * @param negativeValuePolicy the policy for handling negative values
     */
    public MultiArrayedStock(String baseName, SubscriptRange range, double initialValue, Unit unit,
                              NegativeValuePolicy negativeValuePolicy) {
        this.baseName = baseName;
        this.range = range;
        this.unit = unit;
        this.stocks = new Stock[range.totalSize()];
        for (int i = 0; i < range.totalSize(); i++) {
            stocks[i] = new Stock(range.composeName(baseName, i), initialValue, unit, negativeValuePolicy);
        }
    }

    /**
     * Creates a multi-arrayed stock with per-element initial values in row-major order.
     * Uses {@link NegativeValuePolicy#ALLOW} by default.
     *
     * @param baseName      the base name
     * @param range         the multi-dimensional subscript range
     * @param initialValues the initial values, one per element in row-major order
     * @param unit          the unit of measure
     * @throws IllegalArgumentException if the array length doesn't match the range's total size
     */
    public MultiArrayedStock(String baseName, SubscriptRange range, double[] initialValues, Unit unit) {
        this(baseName, range, initialValues, unit, NegativeValuePolicy.ALLOW);
    }

    /**
     * Creates a multi-arrayed stock with per-element initial values and an explicit negative-value policy.
     *
     * @param baseName            the base name
     * @param range               the multi-dimensional subscript range
     * @param initialValues       the initial values, one per element in row-major order
     * @param unit                the unit of measure
     * @param negativeValuePolicy the policy for handling negative values
     * @throws IllegalArgumentException if the array length doesn't match the range's total size
     */
    public MultiArrayedStock(String baseName, SubscriptRange range, double[] initialValues, Unit unit,
                              NegativeValuePolicy negativeValuePolicy) {
        if (initialValues.length != range.totalSize()) {
            throw new IllegalArgumentException(
                    "Expected " + range.totalSize() + " initial values but got " + initialValues.length);
        }
        this.baseName = baseName;
        this.range = range;
        this.unit = unit;
        this.stocks = new Stock[range.totalSize()];
        for (int i = 0; i < range.totalSize(); i++) {
            stocks[i] = new Stock(range.composeName(baseName, i), initialValues[i], unit, negativeValuePolicy);
        }
    }

    /**
     * Convenience constructor that builds a {@link SubscriptRange} from the given subscript list.
     */
    public MultiArrayedStock(String baseName, List<Subscript> subscripts, double initialValue, Unit unit) {
        this(baseName, new SubscriptRange(subscripts), initialValue, unit);
    }

    /**
     * Convenience constructor that builds a {@link SubscriptRange} from the given subscript list.
     */
    public MultiArrayedStock(String baseName, List<Subscript> subscripts, double[] initialValues, Unit unit) {
        this(baseName, new SubscriptRange(subscripts), initialValues, unit);
    }

    /**
     * Returns the current value of the element at the given flat index.
     */
    public double getValue(int flatIndex) {
        return stocks[flatIndex].getValue();
    }

    /**
     * Returns the underlying stock at the given flat index.
     */
    public Stock getStock(int flatIndex) {
        return stocks[flatIndex];
    }

    /**
     * Returns the current value of the element at the given coordinates.
     */
    public double getValueAt(int... coords) {
        return stocks[range.toFlatIndex(coords)].getValue();
    }

    /**
     * Returns the current value of the element at the given labels.
     */
    public double getValueAt(String... labels) {
        return stocks[range.toFlatIndex(labels)].getValue();
    }

    /**
     * Returns the underlying stock at the given coordinates.
     */
    public Stock getStockAt(int... coords) {
        return stocks[range.toFlatIndex(coords)];
    }

    /**
     * Returns the underlying stock at the given labels.
     */
    public Stock getStockAt(String... labels) {
        return stocks[range.toFlatIndex(labels)];
    }

    /**
     * Returns all underlying stocks as an unmodifiable list.
     */
    public List<Stock> getStocks() {
        return Collections.unmodifiableList(Arrays.asList(stocks));
    }

    /**
     * Returns the sum of all element values.
     */
    public double sum() {
        double total = 0;
        for (Stock stock : stocks) {
            total += stock.getValue();
        }
        return total;
    }

    /**
     * Collapses one dimension by summing over it, returning an array of doubles
     * sized to the product of the remaining dimensions.
     *
     * <p>For a range with dimensions [Region(3), AgeGroup(3)]:
     * <ul>
     *   <li>{@code sumOver(1)} collapses AgeGroup → returns double[3], one per Region</li>
     *   <li>{@code sumOver(0)} collapses Region → returns double[3], one per AgeGroup</li>
     * </ul>
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
            // Build reduced coordinates (skip the collapsed dimension)
            int[] reducedCoords = new int[coords.length - 1];
            int j = 0;
            for (int d = 0; d < coords.length; d++) {
                if (d != dimensionIndex) {
                    reducedCoords[j++] = coords[d];
                }
            }
            int reducedFlat = reducedRange.toFlatIndex(reducedCoords);
            result[reducedFlat] += stocks[flatIdx].getValue();
        }
        return result;
    }

    /**
     * Fixes one dimension at the given label, returning the stocks for all combinations
     * of the remaining dimensions.
     *
     * @param dimension the dimension index to fix
     * @param label     the label value to fix
     * @return an array of stocks for the slice
     */
    public Stock[] slice(int dimension, String label) {
        return slice(dimension, range.getSubscript(dimension).indexOf(label));
    }

    /**
     * Fixes one dimension at the given index, returning the stocks for all combinations
     * of the remaining dimensions.
     *
     * @param dimension the dimension index to fix
     * @param index     the coordinate index within that dimension
     * @return an array of stocks for the slice
     */
    public Stock[] slice(int dimension, int index) {
        int sliceSize = range.totalSize() / range.getSubscript(dimension).size();
        Stock[] result = new Stock[sliceSize];
        int pos = 0;
        for (int flatIdx = 0; flatIdx < range.totalSize(); flatIdx++) {
            int[] coords = range.toCoordinates(flatIdx);
            if (coords[dimension] == index) {
                result[pos++] = stocks[flatIdx];
            }
        }
        return result;
    }

    /**
     * Connects each flow in the multi-arrayed flow as an inflow to the corresponding stock (1:1 by flat index).
     *
     * @throws IllegalArgumentException if the flow's size doesn't match this stock's size
     */
    public void addInflow(MultiArrayedFlow multiArrayedFlow) {
        if (multiArrayedFlow.size() != stocks.length) {
            throw new IllegalArgumentException(
                    "Flow size (" + multiArrayedFlow.size() + ") does not match stock size (" + stocks.length + ")");
        }
        for (int i = 0; i < stocks.length; i++) {
            stocks[i].addInflow(multiArrayedFlow.getFlow(i));
        }
    }

    /**
     * Connects each flow in the multi-arrayed flow as an outflow from the corresponding stock (1:1 by flat index).
     *
     * @throws IllegalArgumentException if the flow's size doesn't match this stock's size
     */
    public void addOutflow(MultiArrayedFlow multiArrayedFlow) {
        if (multiArrayedFlow.size() != stocks.length) {
            throw new IllegalArgumentException(
                    "Flow size (" + multiArrayedFlow.size() + ") does not match stock size (" + stocks.length + ")");
        }
        for (int i = 0; i < stocks.length; i++) {
            stocks[i].addOutflow(multiArrayedFlow.getFlow(i));
        }
    }

    /**
     * Wiring a single scalar flow to a multi-arrayed stock is not supported because the flow's
     * computed value would be applied to each of the N underlying stocks.
     *
     * <p>Use {@link #addInflow(MultiArrayedFlow)} or wire individual stocks via
     * {@link #getStockAt(int...)}.
     *
     * @param flow unused — always throws
     * @throws UnsupportedOperationException always
     */
    public void addInflow(Flow flow) {
        throw new UnsupportedOperationException(
                "Cannot wire a single scalar flow to a multi-arrayed stock — the flow would be applied "
                + size() + " times. Use addInflow(MultiArrayedFlow) or wire individual stocks.");
    }

    /**
     * Wiring a single scalar flow to a multi-arrayed stock is not supported because the flow's
     * computed value would be applied to each of the N underlying stocks.
     *
     * <p>Use {@link #addOutflow(MultiArrayedFlow)} or wire individual stocks via
     * {@link #getStockAt(int...)}.
     *
     * @param flow unused — always throws
     * @throws UnsupportedOperationException always
     */
    public void addOutflow(Flow flow) {
        throw new UnsupportedOperationException(
                "Cannot wire a single scalar flow to a multi-arrayed stock — the flow would be applied "
                + size() + " times. Use addOutflow(MultiArrayedFlow) or wire individual stocks.");
    }

    /**
     * Returns the number of elements (same as {@code range.totalSize()}).
     */
    public int size() {
        return stocks.length;
    }

    /**
     * Returns a snapshot of the current stock values as an {@link IndexedValue}.
     */
    public IndexedValue getIndexedValue() {
        double[] vals = new double[stocks.length];
        for (int i = 0; i < stocks.length; i++) {
            vals[i] = stocks[i].getValue();
        }
        return IndexedValue.of(range, vals);
    }

    /**
     * Returns the multi-dimensional subscript range used by this arrayed stock.
     */
    public SubscriptRange getRange() {
        return range;
    }

    /**
     * Returns the base name shared by all underlying stocks.
     */
    public String getBaseName() {
        return baseName;
    }

    /**
     * Returns the unit of measure for elements of this arrayed stock.
     */
    public Unit getUnit() {
        return unit;
    }

    @Override
    public String toString() {
        return "MultiArrayedStock(" + baseName + ", " + range + ")";
    }
}

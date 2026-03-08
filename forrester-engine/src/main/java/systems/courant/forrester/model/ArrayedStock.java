package systems.courant.forrester.model;

import systems.courant.forrester.measure.Unit;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * An arrayed stock that wraps N plain {@link Stock} instances — one per element of a
 * {@link Subscript} dimension.
 *
 * <p>Each underlying stock is named {@code "baseName[label]"} (e.g., {@code "Population[North]"}).
 * When added to a {@link Model} via {@link Model#addArrayedStock(ArrayedStock)}, the individual
 * stocks are expanded into the model's flat stock list, so the simulation loop and all output
 * infrastructure work without changes.
 */
public class ArrayedStock {

    private final String baseName;
    private final Subscript subscript;
    private final Stock[] stocks;
    private final Unit unit;

    /**
     * Creates an arrayed stock with the same initial value for every element.
     * Uses {@link NegativeValuePolicy#CLAMP_TO_ZERO} by default.
     *
     * @param baseName     the base name (each stock is named "baseName[label]")
     * @param subscript    the subscript dimension
     * @param initialValue the initial value for every element
     * @param unit         the unit of measure
     */
    public ArrayedStock(String baseName, Subscript subscript, double initialValue, Unit unit) {
        this(baseName, subscript, initialValue, unit, NegativeValuePolicy.CLAMP_TO_ZERO);
    }

    /**
     * Creates an arrayed stock with the same initial value for every element and
     * an explicit negative-value policy.
     *
     * @param baseName            the base name
     * @param subscript           the subscript dimension
     * @param initialValue        the initial value for every element
     * @param unit                the unit of measure
     * @param negativeValuePolicy the policy for handling negative values
     */
    public ArrayedStock(String baseName, Subscript subscript, double initialValue, Unit unit,
                         NegativeValuePolicy negativeValuePolicy) {
        this.baseName = baseName;
        this.subscript = subscript;
        this.unit = unit;
        this.stocks = new Stock[subscript.size()];
        for (int i = 0; i < subscript.size(); i++) {
            stocks[i] = new Stock(baseName + "[" + subscript.getLabel(i) + "]", initialValue, unit,
                    negativeValuePolicy);
        }
    }

    /**
     * Creates an arrayed stock with per-element initial values.
     * Uses {@link NegativeValuePolicy#CLAMP_TO_ZERO} by default.
     *
     * @param baseName      the base name (each stock is named "baseName[label]")
     * @param subscript     the subscript dimension
     * @param initialValues the initial values, one per subscript element
     * @param unit          the unit of measure
     * @throws IllegalArgumentException if the array length doesn't match the subscript size
     */
    public ArrayedStock(String baseName, Subscript subscript, double[] initialValues, Unit unit) {
        this(baseName, subscript, initialValues, unit, NegativeValuePolicy.CLAMP_TO_ZERO);
    }

    /**
     * Creates an arrayed stock with per-element initial values and an explicit negative-value policy.
     *
     * @param baseName            the base name
     * @param subscript           the subscript dimension
     * @param initialValues       the initial values, one per subscript element
     * @param unit                the unit of measure
     * @param negativeValuePolicy the policy for handling negative values
     * @throws IllegalArgumentException if the array length doesn't match the subscript size
     */
    public ArrayedStock(String baseName, Subscript subscript, double[] initialValues, Unit unit,
                         NegativeValuePolicy negativeValuePolicy) {
        if (initialValues.length != subscript.size()) {
            throw new IllegalArgumentException(
                    "Expected " + subscript.size() + " initial values but got " + initialValues.length);
        }
        this.baseName = baseName;
        this.subscript = subscript;
        this.unit = unit;
        this.stocks = new Stock[subscript.size()];
        for (int i = 0; i < subscript.size(); i++) {
            stocks[i] = new Stock(baseName + "[" + subscript.getLabel(i) + "]", initialValues[i], unit,
                    negativeValuePolicy);
        }
    }

    /**
     * Returns the current value of the element at the given index.
     */
    public double getValue(int index) {
        return stocks[index].getValue();
    }

    /**
     * Returns the current value of the element with the given label.
     */
    public double getValue(String label) {
        return stocks[subscript.indexOf(label)].getValue();
    }

    /**
     * Returns the underlying stock at the given index.
     */
    public Stock getStock(int index) {
        return stocks[index];
    }

    /**
     * Returns the underlying stock with the given label.
     */
    public Stock getStock(String label) {
        return stocks[subscript.indexOf(label)];
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
     * Returns the number of elements.
     */
    public int size() {
        return stocks.length;
    }

    /**
     * Connects each flow in the arrayed flow as an inflow to the corresponding stock.
     *
     * @param arrayedFlow the arrayed flow whose elements become inflows
     * @throws IllegalArgumentException if the flow's size doesn't match this stock's size
     */
    public void addInflow(ArrayedFlow arrayedFlow) {
        if (arrayedFlow.size() != stocks.length) {
            throw new IllegalArgumentException(
                    "Flow size (" + arrayedFlow.size() + ") does not match stock size (" + stocks.length + ")");
        }
        for (int i = 0; i < stocks.length; i++) {
            stocks[i].addInflow(arrayedFlow.getFlow(i));
        }
    }

    /**
     * Connects each flow in the arrayed flow as an outflow from the corresponding stock.
     *
     * @param arrayedFlow the arrayed flow whose elements become outflows
     * @throws IllegalArgumentException if the flow's size doesn't match this stock's size
     */
    public void addOutflow(ArrayedFlow arrayedFlow) {
        if (arrayedFlow.size() != stocks.length) {
            throw new IllegalArgumentException(
                    "Flow size (" + arrayedFlow.size() + ") does not match stock size (" + stocks.length + ")");
        }
        for (int i = 0; i < stocks.length; i++) {
            stocks[i].addOutflow(arrayedFlow.getFlow(i));
        }
    }

    /**
     * Wiring a single scalar flow to an arrayed stock is not supported because the flow's
     * computed value would be applied to each of the N underlying stocks, effectively
     * multiplying the flow's effect by N.
     *
     * <p>Use {@link #addInflow(ArrayedFlow)} with a per-element arrayed flow instead,
     * or wire individual stocks directly via {@link #getStock(int)}.
     *
     * @param flow unused — always throws
     * @throws UnsupportedOperationException always
     */
    public void addInflow(Flow flow) {
        throw new UnsupportedOperationException(
                "Cannot wire a single scalar flow to an arrayed stock — the flow would be applied "
                + size() + " times. Use addInflow(ArrayedFlow) or wire individual stocks via getStock(i).");
    }

    /**
     * Wiring a single scalar flow to an arrayed stock is not supported because the flow's
     * computed value would be applied to each of the N underlying stocks, effectively
     * multiplying the flow's effect by N.
     *
     * <p>Use {@link #addOutflow(ArrayedFlow)} with a per-element arrayed flow instead,
     * or wire individual stocks directly via {@link #getStock(int)}.
     *
     * @param flow unused — always throws
     * @throws UnsupportedOperationException always
     */
    public void addOutflow(Flow flow) {
        throw new UnsupportedOperationException(
                "Cannot wire a single scalar flow to an arrayed stock — the flow would be applied "
                + size() + " times. Use addOutflow(ArrayedFlow) or wire individual stocks via getStock(i).");
    }

    /**
     * Returns a snapshot of the current stock values as an {@link IndexedValue}.
     */
    public IndexedValue getIndexedValue() {
        double[] vals = new double[stocks.length];
        for (int i = 0; i < stocks.length; i++) {
            vals[i] = stocks[i].getValue();
        }
        return IndexedValue.of(subscript, vals);
    }

    /**
     * Returns the subscript dimension used by this arrayed stock.
     */
    public Subscript getSubscript() {
        return subscript;
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
        return "ArrayedStock(" + baseName + ", " + subscript + ")";
    }
}

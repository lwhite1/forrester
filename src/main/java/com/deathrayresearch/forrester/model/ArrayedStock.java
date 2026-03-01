package com.deathrayresearch.forrester.model;

import com.deathrayresearch.forrester.measure.Unit;

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
     * Connects a single scalar flow as an inflow to ALL underlying stocks.
     * <p><strong>Note:</strong> The flow's {@code sink} will be set to the last stock in the array
     * because {@link Stock#addInflow(Flow)} calls {@code flow.setSink(this)} for each stock.
     * This is acceptable when the flow formula does not depend on its sink reference.
     * For per-element flow wiring, use {@link #addInflow(ArrayedFlow)} instead.
     *
     * @param flow the scalar flow to add as inflow to every element
     */
    public void addInflow(Flow flow) {
        for (Stock stock : stocks) {
            stock.addInflow(flow);
        }
    }

    /**
     * Connects a single scalar flow as an outflow from ALL underlying stocks.
     * <p><strong>Note:</strong> The flow's {@code source} will be set to the last stock in the array
     * because {@link Stock#addOutflow(Flow)} calls {@code flow.setSource(this)} for each stock.
     * This is acceptable when the flow formula does not depend on its source reference.
     * For per-element flow wiring, use {@link #addOutflow(ArrayedFlow)} instead.
     *
     * @param flow the scalar flow to add as outflow from every element
     */
    public void addOutflow(Flow flow) {
        for (Stock stock : stocks) {
            stock.addOutflow(flow);
        }
    }

    public Subscript getSubscript() {
        return subscript;
    }

    public String getBaseName() {
        return baseName;
    }

    public Unit getUnit() {
        return unit;
    }

    @Override
    public String toString() {
        return "ArrayedStock(" + baseName + ", " + subscript + ")";
    }
}

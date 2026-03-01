package com.deathrayresearch.forrester.model;

import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.Unit;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A level or store of some quantity of interest
 */
public class Stock extends Element {

    private final Set<Flow> inflows = new LinkedHashSet<>();
    private final Set<Flow> outflows = new LinkedHashSet<>();

    private final Unit unit;
    private double value;
    private NegativeValuePolicy negativeValuePolicy;

    /**
     * Creates a new stock with the given name, initial value, and unit.
     * Uses {@link NegativeValuePolicy#CLAMP_TO_ZERO} by default.
     *
     * @param name          the stock name
     * @param initialAmount the initial quantity stored in this stock
     * @param unit          the unit of measure for the stored quantity
     */
    public Stock(String name, double initialAmount, Unit unit) {
        this(name, initialAmount, unit, NegativeValuePolicy.CLAMP_TO_ZERO);
    }

    /**
     * Creates a new stock with the given name, initial value, unit, and negative-value policy.
     *
     * @param name                the stock name
     * @param initialAmount       the initial quantity stored in this stock
     * @param unit                the unit of measure for the stored quantity
     * @param negativeValuePolicy the policy for handling negative values
     */
    public Stock(String name, double initialAmount, Unit unit, NegativeValuePolicy negativeValuePolicy) {
        super(name);
        this.unit = unit;
        this.negativeValuePolicy = negativeValuePolicy;
        this.value = applyPolicy(initialAmount);
    }

    /**
     * Registers the given flow as an inflow to this stock and sets this stock as the flow's sink.
     *
     * @param inFlow the flow to add as an inflow
     */
    public void addInflow(Flow inFlow) {
        inflows.add(inFlow);
        inFlow.setSink(this);
    }

    /**
     * Registers the given flow as an outflow from this stock and sets this stock as the flow's source.
     *
     * @param outFlow the flow to add as an outflow
     */
    public void addOutflow(Flow outFlow) {
        outflows.add(outFlow);
        outFlow.setSource(this);
    }

    /**
     * Returns an unmodifiable view of the inflows feeding into this stock.
     */
    public Set<Flow> getInflows() {
        return Collections.unmodifiableSet(inflows);
    }

    /**
     * Returns an unmodifiable view of the outflows draining from this stock.
     */
    public Set<Flow> getOutflows() {
        return Collections.unmodifiableSet(outflows);
    }

    /**
     * Returns the current value of this stock as a {@link Quantity}.
     */
    public Quantity getQuantity() {
        return new Quantity(value, unit);
    }

    public Unit getUnit() {
        return unit;
    }

    public void setValue(double value) {
        this.value = applyPolicy(value);
    }

    public double getValue() {
        return value;
    }

    public NegativeValuePolicy getNegativeValuePolicy() {
        return negativeValuePolicy;
    }

    public void setNegativeValuePolicy(NegativeValuePolicy negativeValuePolicy) {
        this.negativeValuePolicy = negativeValuePolicy;
    }

    private double applyPolicy(double candidateValue) {
        if (Double.isNaN(candidateValue) || Double.isInfinite(candidateValue)) {
            throw new IllegalArgumentException(
                    "Stock '" + getName() + "' received non-finite value: " + candidateValue);
        }
        if (candidateValue >= 0) {
            return candidateValue;
        }
        return switch (negativeValuePolicy) {
            case ALLOW -> candidateValue;
            case CLAMP_TO_ZERO -> 0;
            case THROW -> throw new IllegalArgumentException(
                    "Stock '" + getName() + "' cannot have a negative value: " + candidateValue);
        };
    }

    @Override
    public String toString() {
        return "Stock (" + getName() +"): " + getQuantity();
    }
}

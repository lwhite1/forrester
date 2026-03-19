package systems.courant.sd.model;

import systems.courant.sd.measure.Quantity;
import systems.courant.sd.measure.Unit;
import com.carrotsearch.hppc.DoubleArrayList;
import com.google.common.base.Preconditions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A level or store of some quantity of interest
 */
public class Stock extends Element {

    private static final Logger log = LoggerFactory.getLogger(Stock.class);

    private final Set<Flow> inflows = new LinkedHashSet<>();
    private final Set<Flow> outflows = new LinkedHashSet<>();
    private final DoubleArrayList history = new DoubleArrayList();

    private final Unit unit;
    private final double initialAmount;
    private double value;
    private boolean warnedNonFinite;
    private final NegativeValuePolicy negativeValuePolicy;

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
        Preconditions.checkNotNull(unit, "unit must not be null");
        Preconditions.checkNotNull(negativeValuePolicy, "negativeValuePolicy must not be null");
        Preconditions.checkArgument(Double.isFinite(initialAmount),
                "Stock '%s' initial value must be finite, got: %s", name, initialAmount);
        this.unit = unit;
        this.negativeValuePolicy = negativeValuePolicy;
        this.initialAmount = initialAmount;
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

    /**
     * Returns the unit of measure for the quantity stored in this stock.
     */
    public Unit getUnit() {
        return unit;
    }

    /**
     * Sets the current value of this stock, applying the configured
     * {@link NegativeValuePolicy} if the value is negative. Non-finite values
     * (NaN, Infinity) are rejected with a warning and the previous value is kept,
     * consistent with the Simulation engine's behavior.
     *
     * @param value the new value
     * @throws IllegalArgumentException if the value is negative when the
     *                                  policy is {@link NegativeValuePolicy#THROW}
     */
    public void setValue(double value) {
        if (!Double.isFinite(value)) {
            if (!warnedNonFinite) {
                log.warn("Stock '{}' received non-finite value: {} — keeping previous value ({})",
                        getName(), value, this.value);
                warnedNonFinite = true;
            }
            return;
        }
        this.value = applyPolicy(value);
    }

    /**
     * Returns the current numeric value of this stock.
     */
    public double getValue() {
        return value;
    }

    /**
     * Returns the initial amount this stock was created with.
     */
    public double getInitialAmount() {
        return initialAmount;
    }

    /**
     * Resets this stock's value to the initial amount it was created with,
     * and clears the non-finite warning flag.
     */
    public void resetToInitialValue() {
        this.value = applyPolicy(initialAmount);
        this.warnedNonFinite = false;
    }

    /**
     * Resets the non-finite warning flag so that warnings will fire again
     * on the next simulation re-run.
     */
    public void resetWarnings() {
        warnedNonFinite = false;
    }

    /**
     * Records the current value in this stock's history for the current time step.
     */
    public void recordValue() {
        history.add(value);
    }

    /**
     * Returns the recorded stock value at the given time step index, or 0 if the index is out of range.
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
     * Clears this stock's recorded history. Useful when re-running simulations.
     */
    public void clearHistory() {
        history.clear();
    }

    /**
     * Returns the policy that governs how this stock handles negative values.
     */
    public NegativeValuePolicy getNegativeValuePolicy() {
        return negativeValuePolicy;
    }


    private double applyPolicy(double candidateValue) {
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
        return "Stock (" + getName() + "): " + getQuantity();
    }
}

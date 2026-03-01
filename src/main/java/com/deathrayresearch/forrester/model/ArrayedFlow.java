package com.deathrayresearch.forrester.model;

import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.TimeUnit;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.IntFunction;

/**
 * An arrayed flow that wraps N plain {@link Flow} instances — one per element of a
 * {@link Subscript} dimension.
 *
 * <p>Each underlying flow is named {@code "baseName[label]"} and evaluates an index-aware
 * formula to compute its rate.
 */
public class ArrayedFlow {

    private final String baseName;
    private final Subscript subscript;
    private final Flow[] flows;

    private ArrayedFlow(String baseName, Subscript subscript, Flow[] flows) {
        this.baseName = baseName;
        this.subscript = subscript;
        this.flows = flows;
    }

    /**
     * Creates an arrayed flow with an index-aware formula for each element.
     *
     * @param baseName  the base name (each flow is named "baseName[label]")
     * @param timeUnit  the time unit for the flow rate
     * @param subscript the subscript dimension
     * @param formula   a function from element index to the quantity per time unit
     * @return a new arrayed flow
     */
    public static ArrayedFlow create(String baseName, TimeUnit timeUnit, Subscript subscript,
                                     IntFunction<Quantity> formula) {
        Flow[] flows = new Flow[subscript.size()];
        for (int i = 0; i < subscript.size(); i++) {
            final int index = i;
            flows[i] = Flow.create(
                    baseName + "[" + subscript.getLabel(i) + "]",
                    timeUnit,
                    () -> formula.apply(index)
            );
        }
        return new ArrayedFlow(baseName, subscript, flows);
    }

    /**
     * Convenience factory that captures an arrayed stock reference for use in the formula.
     * Identical to {@link #create(String, TimeUnit, Subscript, IntFunction)} but makes the
     * stock dependency explicit in the API.
     *
     * @param baseName  the base name
     * @param timeUnit  the time unit for the flow rate
     * @param stock     the arrayed stock this flow operates on (for documentation; the formula
     *                  should reference it via closure)
     * @param subscript the subscript dimension
     * @param formula   a function from element index to the quantity per time unit
     * @return a new arrayed flow
     */
    public static ArrayedFlow create(String baseName, TimeUnit timeUnit, ArrayedStock stock,
                                     Subscript subscript, IntFunction<Quantity> formula) {
        return create(baseName, timeUnit, subscript, formula);
    }

    /**
     * Returns the underlying flow at the given index.
     */
    public Flow getFlow(int index) {
        return flows[index];
    }

    /**
     * Returns the underlying flow with the given label.
     */
    public Flow getFlow(String label) {
        return flows[subscript.indexOf(label)];
    }

    /**
     * Returns all underlying flows as an unmodifiable list.
     */
    public List<Flow> getFlows() {
        return Collections.unmodifiableList(Arrays.asList(flows));
    }

    /**
     * Returns the number of elements.
     */
    public int size() {
        return flows.length;
    }

    public String getBaseName() {
        return baseName;
    }

    public Subscript getSubscript() {
        return subscript;
    }

    @Override
    public String toString() {
        return "ArrayedFlow(" + baseName + ", " + subscript + ")";
    }
}

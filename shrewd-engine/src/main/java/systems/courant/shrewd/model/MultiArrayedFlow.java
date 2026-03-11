package systems.courant.shrewd.model;

import systems.courant.shrewd.measure.Quantity;
import systems.courant.shrewd.measure.TimeUnit;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.IntFunction;

/**
 * A multi-dimensional arrayed flow that wraps N×M×... plain {@link Flow} instances — one per
 * combination of labels across all {@link Subscript} dimensions in a {@link SubscriptRange}.
 *
 * <p>Each underlying flow is named using comma-separated labels, e.g., {@code "Births[North,Young]"},
 * and evaluates a formula to compute its rate.
 */
public class MultiArrayedFlow {

    private final String baseName;
    private final SubscriptRange range;
    private final Flow[] flows;

    private MultiArrayedFlow(String baseName, SubscriptRange range, Flow[] flows) {
        this.baseName = baseName;
        this.range = range;
        this.flows = flows;
    }

    /**
     * Creates a multi-arrayed flow with a coordinate-aware formula for each element.
     *
     * @param baseName the base name (each flow is named "baseName[label0,label1,...]")
     * @param timeUnit the time unit for the flow rate
     * @param range    the multi-dimensional subscript range
     * @param formula  a function from coordinate array to the quantity per time unit
     * @return a new multi-arrayed flow
     */
    public static MultiArrayedFlow create(String baseName, TimeUnit timeUnit, SubscriptRange range,
                                           Function<int[], Quantity> formula) {
        Flow[] flows = new Flow[range.totalSize()];
        for (int i = 0; i < range.totalSize(); i++) {
            final int[] coords = range.toCoordinates(i);
            flows[i] = Flow.create(
                    range.composeName(baseName, i),
                    timeUnit,
                    () -> formula.apply(coords)
            );
        }
        return new MultiArrayedFlow(baseName, range, flows);
    }

    /**
     * Creates a multi-arrayed flow with a flat-index formula for each element.
     *
     * @param baseName the base name
     * @param timeUnit the time unit for the flow rate
     * @param range    the multi-dimensional subscript range
     * @param formula  a function from flat index to the quantity per time unit
     * @return a new multi-arrayed flow
     */
    public static MultiArrayedFlow createByIndex(String baseName, TimeUnit timeUnit, SubscriptRange range,
                                                  IntFunction<Quantity> formula) {
        Flow[] flows = new Flow[range.totalSize()];
        for (int i = 0; i < range.totalSize(); i++) {
            final int index = i;
            flows[i] = Flow.create(
                    range.composeName(baseName, i),
                    timeUnit,
                    () -> formula.apply(index)
            );
        }
        return new MultiArrayedFlow(baseName, range, flows);
    }

    /**
     * Convenience factory identical to {@link #create(String, TimeUnit, SubscriptRange, Function)}
     * that accepts a stock parameter for API documentation purposes. The stock should be referenced
     * via closure in the formula; the parameter itself is not used.
     */
    public static MultiArrayedFlow create(String baseName, TimeUnit timeUnit, MultiArrayedStock stock,
                                           SubscriptRange range, Function<int[], Quantity> formula) {
        return create(baseName, timeUnit, range, formula);
    }

    /**
     * Returns the underlying flow at the given flat index.
     */
    public Flow getFlow(int flatIndex) {
        return flows[flatIndex];
    }

    /**
     * Returns the underlying flow at the given coordinates.
     */
    public Flow getFlowAt(int... coords) {
        return flows[range.toFlatIndex(coords)];
    }

    /**
     * Returns the underlying flow at the given labels.
     */
    public Flow getFlowAt(String... labels) {
        return flows[range.toFlatIndex(labels)];
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

    /**
     * Returns the base name shared by all underlying flows.
     */
    public String getBaseName() {
        return baseName;
    }

    /**
     * Returns the multi-dimensional subscript range used by this arrayed flow.
     */
    public SubscriptRange getRange() {
        return range;
    }

    @Override
    public String toString() {
        return "MultiArrayedFlow(" + baseName + ", " + range + ")";
    }
}

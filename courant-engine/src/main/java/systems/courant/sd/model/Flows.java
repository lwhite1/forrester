package systems.courant.sd.model;

import systems.courant.sd.measure.Quantity;
import systems.courant.sd.measure.TimeUnit;

import com.google.common.base.Preconditions;

import java.util.function.LongSupplier;

/**
 * Factory methods for creating common system dynamics flow patterns.
 *
 * <p>Each method returns a fully constructed {@link Flow} instance, eliminating the need
 * for anonymous subclasses and archetype helper classes. All methods delegate to
 * {@link Flow#create(String, TimeUnit, java.util.function.Supplier)}.
 */
public final class Flows {

    private Flows() {
        // prevent instantiation
    }

    /**
     * Creates a flow that contributes a fixed quantity per time step.
     *
     * @param name     the flow name
     * @param timeUnit the time unit in which the rate is expressed
     * @param quantity the constant quantity per time step
     * @return a new constant flow
     */
    public static Flow constant(String name, TimeUnit timeUnit, Quantity quantity) {
        return Flow.create(name, timeUnit, () -> quantity);
    }

    /**
     * Creates a flow for linear growth or decay. A constant amount is added per time step,
     * regardless of the current stock level.
     *
     * @param name     the flow name
     * @param timeUnit the time unit in which the rate is expressed
     * @param stock    the stock whose unit determines the flow's unit
     * @param amount   the constant amount added per time step
     * @return a new linear growth flow
     */
    public static Flow linearGrowth(String name, TimeUnit timeUnit, Stock stock, double amount) {
        return Flow.create(name, timeUnit, () -> new Quantity(amount, stock.getUnit()));
    }

    /**
     * Creates a flow for exponential growth or decay. The flow rate equals the stock's
     * current value multiplied by a fractional rate. Growth if used as an inflow, decay
     * if used as an outflow.
     *
     * @param name     the flow name
     * @param timeUnit the time unit in which the rate is expressed
     * @param stock    the stock whose current value drives the rate
     * @param rate     the fractional rate per time step (e.g. 0.04 for 4%)
     * @return a new exponential growth flow
     */
    public static Flow exponentialGrowth(String name, TimeUnit timeUnit, Stock stock, double rate) {
        return Flow.create(name, timeUnit,
                () -> new Quantity(stock.getQuantity().getValue() * rate, stock.getUnit()));
    }

    /**
     * Creates a flow for exponential growth that declines as it approaches a limit, producing
     * an S-shaped curve. The formula is {@code stock * rate * (1 - stock/limit)}.
     *
     * <p>Adoption in a market of fixed size is one example application. Adoption starts slow,
     * increases rapidly as awareness spreads, then slows again as it approaches the limit of
     * the addressable market.
     *
     * @param name     the flow name
     * @param timeUnit the time unit in which the rate is expressed
     * @param stock    the stock whose current value drives the rate
     * @param rate     the fractional rate per time step
     * @param limit    the carrying capacity or upper bound
     * @return a new logistic growth flow
     */
    public static Flow exponentialGrowthWithLimit(String name, TimeUnit timeUnit, Stock stock,
                                                  double rate, double limit) {
        Preconditions.checkArgument(limit > 0, "limit must be positive, but got %s", limit);
        return Flow.create(name, timeUnit, () -> {
            double value = stock.getQuantity().getValue();
            double result = value * rate * (1 - value / limit);
            return new Quantity(result, stock.getUnit());
        });
    }

    /**
     * Creates a flow for a pipeline delay: the kind of delay seen in an assembly line where units
     * pass through in FIFO order and the delay is a constant number of time steps. This formula
     * is for the outflow from a stock, representing departures from the pipeline.
     *
     * <p>The delay is calculated as an offset from the inflow to the same stock.
     *
     * @param name         the flow name
     * @param timeUnit     the time unit in which the rate is expressed
     * @param inflow       the inflow whose history is replayed after the delay
     * @param stepSupplier a supplier for the current simulation step (e.g. {@code simulation::getCurrentStep})
     * @param delay        the number of time steps to delay
     * @return a new pipeline delay flow
     */
    public static Flow pipelineDelay(String name, TimeUnit timeUnit, Flow inflow,
                                     LongSupplier stepSupplier, long delay) {
        return Flow.create(name, timeUnit, () -> {
            long referenceStep = stepSupplier.getAsLong() - delay;
            double value = inflow.getHistoryAtTimeStep((int) referenceStep);
            Stock sink = inflow.getSink();
            Preconditions.checkArgument(sink != null, "inflow must have a sink stock assigned");
            return new Quantity(value, sink.getUnit());
        });
    }
}

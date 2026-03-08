package systems.courant.forrester;

import systems.courant.forrester.event.EventHandler;
import systems.courant.forrester.event.SimulationEndEvent;
import systems.courant.forrester.event.SimulationStartEvent;
import systems.courant.forrester.event.TimeStepEvent;
import systems.courant.forrester.measure.Quantity;
import systems.courant.forrester.measure.TimeUnit;
import systems.courant.forrester.model.Model;
import systems.courant.forrester.model.Stock;
import systems.courant.forrester.model.Flow;
import systems.courant.forrester.model.Variable;
import systems.courant.forrester.measure.Dimension;
import com.google.common.base.Preconditions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Simulation is the execution environment for a model.
 *
 * <p>The simulation runs from step 0 through step N (inclusive), where N is the total number
 * of steps computed from the duration and time step. This means a simulation of 5 days with
 * a 1-day time step runs 6 steps (0, 1, 2, 3, 4, 5), capturing both the initial state and
 * the state after 5 elapsed time steps.
 *
 * <p>At each step, events are fired <em>before</em> stocks are updated, so event handlers
 * observe the stock values from the previous step (or initial values at step 0).
 *
 * <p><strong>Threading:</strong> This class is not thread-safe. A single {@code Simulation}
 * instance must be accessed from one thread at a time. The {@link #execute()} method runs
 * the entire simulation loop synchronously on the calling thread.
 */
public class Simulation {

    private static final Logger log = LoggerFactory.getLogger(Simulation.class);

    /**
     * Maximum number of simulation steps allowed. Protects against unreasonable
     * duration/timeStep combinations that would effectively hang the simulation.
     */
    static final long MAX_STEPS = 10_000_000L;

    /**
     * Default wall-clock timeout for a single simulation run (60 seconds).
     */
    private static final long DEFAULT_TIMEOUT_MS = 60_000L;

    private final Model model;

    private final Quantity duration;

    private final TimeUnit timeStep;

    private final LocalDateTime startTime;

    private long timeoutMs = DEFAULT_TIMEOUT_MS;

    private int currentStep = 0;

    private LocalDateTime currentDateTime;

    private Duration elapsedTime = Duration.ZERO;

    private final List<EventHandler> eventHandlers = new ArrayList<>();

    private final Set<String> warnedNonFiniteStocks = new java.util.HashSet<>();

    public Simulation(Model model, TimeUnit timeStep, Quantity duration) {
        this(model, timeStep, duration, LocalDateTime.now());
    }

    public Simulation(Model model, TimeUnit timeStep, TimeUnit durationUnits, double durationAmount) {
        this(model, timeStep, new Quantity(durationAmount, durationUnits));
    }

    public Simulation(Model model, TimeUnit timeStep, Quantity duration, LocalDateTime startTime) {
        Preconditions.checkNotNull(model, "model must not be null");
        Preconditions.checkNotNull(timeStep, "timeStep must not be null");
        Preconditions.checkNotNull(duration, "duration must not be null");
        Preconditions.checkNotNull(startTime, "startTime must not be null");
        Preconditions.checkArgument(duration.getValue() > 0,
                "duration must be positive, but got %s", duration.getValue());
        Preconditions.checkArgument(duration.getDimension().equals(Dimension.TIME),
                "duration must be a TIME quantity, but got dimension %s", duration.getDimension());
        this.model = model;
        this.timeStep = timeStep;
        this.duration = duration;
        this.startTime = startTime;
        this.currentDateTime = startTime;
    }

    public void addEventHandler(EventHandler handler) {
        eventHandlers.add(handler);
    }

    public void removeEventHandler(EventHandler handler) {
        eventHandlers.remove(handler);
    }

    /**
     * Sets the wall-clock timeout for this simulation. If the simulation loop
     * exceeds this duration, it throws {@link SimulationTimeoutException}.
     * Set to 0 to disable the timeout.
     *
     * @param timeoutMs timeout in milliseconds (default 60,000)
     */
    public void setTimeoutMs(long timeoutMs) {
        Preconditions.checkArgument(timeoutMs >= 0, "timeoutMs must be non-negative");
        this.timeoutMs = timeoutMs;
    }

    public void execute() {
        // Reset state so the simulation can be re-run
        currentStep = 0;
        currentDateTime = startTime;
        elapsedTime = Duration.ZERO;
        warnedNonFiniteStocks.clear();
        clearHistory();

        fireStartEvent(new SimulationStartEvent(this));

        double durationInBaseUnits = duration.getUnit().ratioToBaseUnit();
        long totalSteps = Math.round(
                (duration.getValue() * durationInBaseUnits) / timeStep.ratioToBaseUnit());

        if (totalSteps > MAX_STEPS) {
            throw new IllegalArgumentException(
                    "Simulation requires " + totalSteps + " steps (max " + MAX_STEPS
                            + "). Increase the time step or reduce the duration.");
        }
        if (totalSteps < 0) {
            throw new IllegalArgumentException(
                    "Simulation step count overflowed (computed " + totalSteps
                            + "). Check duration and time step values.");
        }

        long deadlineMs = timeoutMs > 0 ? System.currentTimeMillis() + timeoutMs : Long.MAX_VALUE;

        try {
            while (currentStep <= totalSteps) {
                if (Thread.interrupted()) {
                    log.info("Simulation cancelled at step {}/{}", currentStep, totalSteps);
                    throw new SimulationCancelledException(
                            "Simulation cancelled at step " + currentStep);
                }

                if (currentStep % 10_000 == 0 && System.currentTimeMillis() > deadlineMs) {
                    log.warn("Simulation timed out after {}ms at step {}/{}",
                            timeoutMs, currentStep, totalSteps);
                    throw new SimulationTimeoutException(
                            "Simulation timed out after " + (timeoutMs / 1000)
                                    + " seconds at step " + currentStep + "/" + totalSteps);
                }

                Map<Flow, Quantity> flowMap = new IdentityHashMap<>();

                fireTimeStepEvent(new TimeStepEvent(currentDateTime, model, currentStep, timeStep));
                recordVariableValues();
                updateStocks(flowMap, model.getStocks());
                addStep(currentDateTime);
                currentStep++;
            }
        } finally {
            fireEndEvent(new SimulationEndEvent(model));
        }
    }

    private void fireStartEvent(SimulationStartEvent event) {
        for (EventHandler handler : eventHandlers) {
            handler.handleSimulationStartEvent(event);
        }
    }

    private void fireTimeStepEvent(TimeStepEvent event) {
        for (EventHandler handler : eventHandlers) {
            handler.handleTimeStepEvent(event);
        }
    }

    private void fireEndEvent(SimulationEndEvent event) {
        for (EventHandler handler : eventHandlers) {
            handler.handleSimulationEndEvent(event);
        }
    }

    private void updateStocks(Map<Flow, Quantity> flowMap, List<Stock> stocks) {
        // Phase 1: Compute all flow rates and net deltas using pre-step stock values.
        // This ensures standard Euler integration where all stocks see the same
        // time-step snapshot, regardless of processing order.
        Map<Stock, Double> deltas = new IdentityHashMap<>();
        for (Stock stock : stocks) {
            double delta = 0.0;
            delta += computeFlowDelta(true, flowMap, stock.getInflows());
            delta += computeFlowDelta(false, flowMap, stock.getOutflows());
            deltas.put(stock, delta);
        }

        // Phase 2: Apply all deltas simultaneously.
        for (Stock stock : stocks) {
            double oldValue = stock.getQuantity().getValue();
            double newValue = oldValue + deltas.get(stock);
            if (!Double.isFinite(newValue)) {
                if (warnedNonFiniteStocks.add(stock.getName())) {
                    log.warn("Stock '{}' became {} at step {} (previous value: {}, delta: {})"
                                    + " — keeping previous value",
                            stock.getName(), newValue, currentStep, oldValue, deltas.get(stock));
                }
                // Keep the previous value instead of crashing
                continue;
            }
            stock.setValue(newValue);
        }
    }

    private double computeFlowDelta(boolean isInflow, Map<Flow, Quantity> flows, Set<Flow> flowSet) {
        double delta = 0.0;
        for (Flow flow : flowSet) {
            Quantity q;
            if (flows.containsKey(flow)) {
                q = flows.get(flow);
            } else {
                q = flow.flowPerTimeUnit(timeStep);
                flows.put(flow, q);
                flow.recordValue(q);
            }
            delta += isInflow ? q.getValue() : -q.getValue();
        }
        return delta;
    }

    private void recordVariableValues() {
        for (Variable variable : model.getVariables()) {
            variable.recordValue();
        }
    }

    private void addStep(LocalDateTime dateTime) {
        long nanos = Math.round(timeStep.ratioToBaseUnit() * 1_000_000_000L);
        Duration stepDuration = Duration.ofNanos(nanos);
        currentDateTime = dateTime.plus(stepDuration);
        elapsedTime = elapsedTime.plus(stepDuration);
    }

    public Model getModel() {
        return model;
    }

    public Quantity getDuration() {
        return duration;
    }

    public TimeUnit getTimeStep() {
        return timeStep;
    }

    public LocalDateTime getCurrentDateTime() {
        return currentDateTime;
    }

    public Duration getElapsedTime() {
        return elapsedTime;
    }

    public int getCurrentStep() {
        return currentStep;
    }

    /**
     * Clears recorded history from all flows and variables in the model.
     * Call this before re-running a simulation to avoid stale history data.
     */
    public void clearHistory() {
        for (Flow flow : model.getFlows()) {
            flow.clearHistory();
        }
        for (Variable variable : model.getVariables()) {
            variable.clearHistory();
        }
    }
}

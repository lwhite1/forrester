package systems.courant.sd;

import systems.courant.sd.event.EventHandler;
import systems.courant.sd.event.SimulationEndEvent;
import systems.courant.sd.event.SimulationStartEvent;
import systems.courant.sd.event.TimeStepEvent;
import systems.courant.sd.measure.Quantity;
import systems.courant.sd.measure.TimeUnit;
import systems.courant.sd.model.Formula;
import systems.courant.sd.model.Model;
import systems.courant.sd.model.Module;
import systems.courant.sd.model.Stock;
import systems.courant.sd.model.Flow;
import systems.courant.sd.model.Variable;
import systems.courant.sd.model.compile.Resettable;
import systems.courant.sd.measure.Dimension;
import com.google.common.base.Preconditions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
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

    private boolean strictMode;

    private long savePer = 1;

    private double dt = 1.0;

    private long currentStep = 0;

    private LocalDateTime currentDateTime;

    private Duration elapsedTime = Duration.ZERO;

    private final List<EventHandler> eventHandlers = new ArrayList<>();


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
        Preconditions.checkNotNull(handler, "handler must not be null");
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

    /**
     * Enables or disables strict mode. When strict mode is on, the simulation throws
     * {@link NonFiniteValueException} immediately when a non-finite value (NaN or Infinity)
     * is produced in a flow calculation or stock update. When off (default), non-finite
     * values are silently reverted to the previous stock value with a warning.
     *
     * @param strictMode {@code true} to enable fail-fast on non-finite values
     */
    public void setStrictMode(boolean strictMode) {
        this.strictMode = strictMode;
    }

    public boolean isStrictMode() {
        return strictMode;
    }

    /**
     * Sets the recording interval: only every Nth step is recorded to history
     * and fires time step events. A value of 1 (default) records every step.
     * Use higher values for long-running simulations to reduce memory usage.
     *
     * @param savePer the recording interval in steps (must be &ge; 1)
     */
    public void setSavePer(long savePer) {
        Preconditions.checkArgument(savePer >= 1, "savePer must be >= 1, got %s", savePer);
        this.savePer = savePer;
    }

    public long getSavePer() {
        return savePer;
    }

    /**
     * Sets the fractional time step (dt) for the simulation. When dt is less than 1.0,
     * the simulation takes more steps per time unit, producing finer-grained Euler
     * integration. For example, dt = 0.25 with a Year time step means each step
     * advances 0.25 years.
     *
     * @param dt the fractional time step (must be positive and finite, default 1.0)
     */
    public void setDt(double dt) {
        Preconditions.checkArgument(dt > 0 && Double.isFinite(dt),
                "dt must be positive and finite, got %s", dt);
        this.dt = dt;
    }

    public double getDt() {
        return dt;
    }

    public void execute() {
        // Reset state so the simulation can be re-run
        currentStep = 0;
        currentDateTime = startTime;
        elapsedTime = Duration.ZERO;
        clearHistory();
        resetStatefulFormulas();

        long nanos = Math.round(timeStep.ratioToBaseUnit() * dt * 1_000_000_000L);
        if (nanos <= 0) {
            throw new IllegalArgumentException(
                    "Time step too small to represent in nanoseconds: " + timeStep.getName()
                            + " (ratioToBaseUnit=" + timeStep.ratioToBaseUnit() + ")");
        }

        double rawSteps = duration.inBaseUnits().getValue() / (timeStep.ratioToBaseUnit() * dt);
        // Snap to nearest integer if within epsilon (avoids FP off-by-one)
        long totalSteps;
        double nearest = Math.rint(rawSteps);
        if (Math.abs(rawSteps - nearest) < 1e-9) {
            totalSteps = (long) nearest;
        } else {
            totalSteps = (long) Math.floor(rawSteps);
        }

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

        Duration stepDuration = Duration.ofNanos(nanos);
        long deadlineMs = timeoutMs > 0 ? System.currentTimeMillis() + timeoutMs : Long.MAX_VALUE;
        Map<Flow, Quantity> flowMap = new IdentityHashMap<>();
        List<Stock> allStocks = collectAllStocks();
        for (Stock stock : allStocks) {
            stock.resetWarnings();
        }
        Map<Stock, Double> deltas = new IdentityHashMap<>();

        try {
            fireStartEvent(new SimulationStartEvent(this));

            while (currentStep <= totalSteps) {
                if (Thread.interrupted()) {
                    log.info("Simulation cancelled at step {}/{}", currentStep, totalSteps);
                    throw new SimulationCancelledException(
                            "Simulation cancelled at step " + currentStep);
                }

                if (currentStep % 100 == 0 && System.currentTimeMillis() > deadlineMs) {
                    log.warn("Simulation timed out after {}ms at step {}/{}",
                            timeoutMs, currentStep, totalSteps);
                    throw new SimulationTimeoutException(
                            "Simulation timed out after " + (timeoutMs / 1000)
                                    + " seconds at step " + currentStep + "/" + totalSteps);
                }

                flowMap.clear();

                boolean shouldRecord = currentStep % savePer == 0
                        || currentStep == totalSteps;
                if (shouldRecord) {
                    fireTimeStepEvent(new TimeStepEvent(currentDateTime, model, currentStep, timeStep));
                    recordVariableValues();
                }
                updateStocks(flowMap, deltas, allStocks, shouldRecord);
                advanceClock(stepDuration);
                currentStep++;
            }
        } finally {
            fireEndEvent(new SimulationEndEvent(model));
        }
    }

    private void fireStartEvent(SimulationStartEvent event) {
        for (EventHandler handler : List.copyOf(eventHandlers)) {
            handler.handleSimulationStartEvent(event);
        }
    }

    private void fireTimeStepEvent(TimeStepEvent event) {
        for (EventHandler handler : List.copyOf(eventHandlers)) {
            handler.handleTimeStepEvent(event);
        }
    }

    private void fireEndEvent(SimulationEndEvent event) {
        for (EventHandler handler : List.copyOf(eventHandlers)) {
            handler.handleSimulationEndEvent(event);
        }
    }

    /**
     * Collects stocks from the model's own stock list plus any stocks inside
     * modules added via {@link Model#addModulePreserved}. Flattened modules
     * already have their stocks in the model's stock list; preserved modules
     * do not, so we walk the module tree to find them.
     */
    private List<Stock> collectAllStocks() {
        List<Stock> modelStocks = model.getStocks();
        Set<Stock> modelStockSet = Collections.newSetFromMap(new IdentityHashMap<>());
        modelStockSet.addAll(modelStocks);

        List<Stock> allStocks = new ArrayList<>(modelStocks);
        for (Module module : model.getModules()) {
            collectModuleStocks(module, modelStockSet, allStocks);
        }
        return allStocks;
    }

    private static void collectModuleStocks(Module module, Set<Stock> seen, List<Stock> out) {
        for (Stock stock : module.getStocks()) {
            if (seen.add(stock)) {
                out.add(stock);
            }
        }
        for (Module child : module.getSubModules().values()) {
            collectModuleStocks(child, seen, out);
        }
    }

    private void updateStocks(Map<Flow, Quantity> flowMap, Map<Stock, Double> deltas,
                              List<Stock> stocks, boolean shouldRecord) {
        // Phase 1: Compute all flow rates and net deltas using pre-step stock values.
        // This ensures standard Euler integration where all stocks see the same
        // time-step snapshot, regardless of processing order.
        deltas.clear();
        for (Stock stock : stocks) {
            double delta = 0.0;
            delta += computeFlowDelta(true, flowMap, stock.getInflows(), shouldRecord);
            delta += computeFlowDelta(false, flowMap, stock.getOutflows(), shouldRecord);
            deltas.put(stock, delta);
        }

        // Phase 2: Apply all deltas simultaneously.
        // In strict mode, non-finite values throw immediately. Otherwise,
        // Stock.setValue() handles non-finite values by retaining the previous
        // value and logging a warning, so no additional guard is needed.
        for (Stock stock : stocks) {
            double oldValue = stock.getValue();
            double newValue = oldValue + deltas.get(stock) * dt;
            if (strictMode && !Double.isFinite(newValue)) {
                throw new NonFiniteValueException(
                        "Stock '" + stock.getName() + "' became " + newValue
                                + " at step " + currentStep
                                + " (previous value: " + oldValue
                                + ", delta: " + deltas.get(stock) + ")");
            }
            stock.setValue(newValue);
        }
    }

    private double computeFlowDelta(boolean isInflow, Map<Flow, Quantity> flows, Set<Flow> flowSet,
                                    boolean shouldRecord) {
        double delta = 0.0;
        for (Flow flow : flowSet) {
            Quantity q;
            if (flows.containsKey(flow)) {
                q = flows.get(flow);
            } else {
                q = flow.flowPerTimeUnit(timeStep);
                flows.put(flow, q);
                if (Double.isFinite(q.getValue())) {
                    if (shouldRecord) {
                        flow.recordValue(q);
                    }
                } else if (strictMode) {
                    throw new NonFiniteValueException(
                            "Flow '" + flow.getName() + "' produced " + q.getValue()
                                    + " at step " + currentStep);
                }
            }
            delta += isInflow ? q.getValue() : -q.getValue();
        }
        return delta;
    }

    private void recordVariableValues() {
        Set<Variable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Variable variable : model.getVariables()) {
            seen.add(variable);
            variable.recordValue();
        }
        for (Module module : model.getModules()) {
            recordModuleVariableValues(module, seen);
        }
    }

    private static void recordModuleVariableValues(Module module, Set<Variable> seen) {
        for (Variable variable : module.getVariables()) {
            if (seen.add(variable)) {
                variable.recordValue();
            }
        }
        for (Module child : module.getSubModules().values()) {
            recordModuleVariableValues(child, seen);
        }
    }

    private void advanceClock(Duration stepDuration) {
        currentDateTime = currentDateTime.plus(stepDuration);
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

    public long getCurrentStep() {
        return currentStep;
    }

    /**
     * Clears recorded history from all flows and variables in the model.
     * Call this before re-running a simulation to avoid stale history data.
     */
    public void clearHistory() {
        Set<Flow> seenFlows = Collections.newSetFromMap(new IdentityHashMap<>());
        Set<Variable> seenVars = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Flow flow : model.getFlows()) {
            seenFlows.add(flow);
            flow.clearHistory();
        }
        for (Variable variable : model.getVariables()) {
            seenVars.add(variable);
            variable.clearHistory();
        }
        for (Module module : model.getModules()) {
            clearModuleHistory(module, seenFlows, seenVars);
        }
    }

    /**
     * Resets any stateful formulas (Smooth, Delay, Trend, etc.) found in variables
     * throughout the model and its modules. This ensures re-running the simulation
     * produces correct results without requiring an external reset call.
     */
    private void resetStatefulFormulas() {
        Set<Variable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Variable variable : model.getVariables()) {
            seen.add(variable);
            resetIfStateful(variable.getFormula());
        }
        for (Module module : model.getModules()) {
            resetModuleFormulas(module, seen);
        }
    }

    private static void resetModuleFormulas(Module module, Set<Variable> seen) {
        for (Variable variable : module.getVariables()) {
            if (seen.add(variable)) {
                resetIfStateful(variable.getFormula());
            }
        }
        for (Module child : module.getSubModules().values()) {
            resetModuleFormulas(child, seen);
        }
    }

    private static void resetIfStateful(Formula formula) {
        if (formula instanceof Resettable resettable) {
            resettable.reset();
        }
    }

    private static void clearModuleHistory(Module module, Set<Flow> seenFlows, Set<Variable> seenVars) {
        for (Flow flow : module.getFlows()) {
            if (seenFlows.add(flow)) {
                flow.clearHistory();
            }
        }
        for (Variable variable : module.getVariables()) {
            if (seenVars.add(variable)) {
                variable.clearHistory();
            }
        }
        for (Module child : module.getSubModules().values()) {
            clearModuleHistory(child, seenFlows, seenVars);
        }
    }
}

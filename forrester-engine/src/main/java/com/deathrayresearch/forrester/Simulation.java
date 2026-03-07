package com.deathrayresearch.forrester;

import com.deathrayresearch.forrester.event.EventHandler;
import com.deathrayresearch.forrester.event.SimulationEndEvent;
import com.deathrayresearch.forrester.event.SimulationStartEvent;
import com.deathrayresearch.forrester.event.TimeStepEvent;
import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.TimeUnit;
import com.deathrayresearch.forrester.model.Model;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.model.Flow;
import com.deathrayresearch.forrester.model.Variable;
import com.deathrayresearch.forrester.measure.Dimension;
import com.google.common.base.Preconditions;

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

    private final Model model;

    private final Quantity duration;

    private final TimeUnit timeStep;

    private final LocalDateTime startTime;

    private int currentStep = 0;

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
        eventHandlers.add(handler);
    }

    public void removeEventHandler(EventHandler handler) {
        eventHandlers.remove(handler);
    }

    public void execute() {
        // Reset state so the simulation can be re-run
        currentStep = 0;
        currentDateTime = startTime;
        elapsedTime = Duration.ZERO;
        clearHistory();

        fireStartEvent(new SimulationStartEvent(this));

        double durationInBaseUnits = duration.getUnit().ratioToBaseUnit();
        long totalSteps = Math.round(
                (duration.getValue() * durationInBaseUnits) / timeStep.ratioToBaseUnit());

        try {
            while (currentStep <= totalSteps) {
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
        for (Stock stock : stocks) {
            handleFlows(true, flowMap, stock, stock.getInflows());
            handleFlows(false, flowMap, stock, stock.getOutflows());
        }
    }

    private void recordVariableValues() {
        for (Variable variable : model.getVariables()) {
            variable.recordValue();
        }
    }

    private void handleFlows(boolean isInflow, Map<Flow, Quantity> flows, Stock stock, Set<Flow> flowSet) {
        for (Flow flow : flowSet) {
            Quantity q;
            if (flows.containsKey(flow)) {
                q = flows.get(flow);
            } else {
                q = flow.flowPerTimeUnit(timeStep);
                flows.put(flow, q);
                flow.recordValue(q);
            }

            Quantity qCurrent = stock.getQuantity();
            if (isInflow) {
                qCurrent = qCurrent.add(q);
            } else {
                qCurrent = qCurrent.subtract(q);
            }

            stock.setValue(qCurrent.getValue());
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

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
import com.google.common.eventbus.EventBus;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Simulation is the execution environment for a model
 */
public class Simulation {

    private final Model model;

    private final Quantity duration;

    private final TimeUnit timeStep;

    private final LocalDateTime startTime;

    private int currentStep = 0;

    private LocalDateTime currentDateTime;

    private Duration elapsedTime = Duration.ZERO;

    private final Set<EventHandler> eventHandlers = new HashSet<>();

    private final EventBus eventBus;

    public Simulation(Model model, TimeUnit timeStep, Quantity duration) {
        this(model, timeStep, duration, LocalDateTime.now());
    }

    public Simulation(Model model, TimeUnit timeStep, TimeUnit durationUnits, double durationAmount) {
        this(model, timeStep, new Quantity(durationAmount, durationUnits));
    }

    public Simulation(Model model, TimeUnit timeStep, Quantity duration, LocalDateTime startTime) {
        this.model = model;
        this.timeStep = timeStep;
        this.duration = duration;
        this.startTime = startTime;
        this.currentDateTime = startTime;
        this.eventBus = new EventBus();
    }

    public void addEventHandler(EventHandler handler) {
        eventBus.register(handler);
        eventHandlers.add(handler);
    }

    public void removeEventHandler(EventHandler handler) {
        eventBus.unregister(handler);
        eventHandlers.remove(handler);
    }

    public EventBus getEventBus() {
        return eventBus;
    }

    public void execute() {
        // Reset state so the simulation can be re-run
        currentStep = 0;
        currentDateTime = startTime;
        elapsedTime = Duration.ZERO;

        eventBus.post(new SimulationStartEvent(this));

        double durationInBaseUnits = duration.getUnit().ratioToBaseUnit();
        long totalSteps = Math.round(
                (duration.getValue() * durationInBaseUnits) / timeStep.ratioToBaseUnit());

        try {
            while (currentStep <= totalSteps) {
                Map<Flow, Quantity> flowMap = new IdentityHashMap<>();

                eventBus.post(new TimeStepEvent(currentDateTime, model, currentStep, timeStep));
                recordVariableValues();
                updateStocks(flowMap, model.getStocks());
                addStep(currentDateTime);
                currentStep++;
            }
        } finally {
            eventBus.post(new SimulationEndEvent());
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
}

package com.deathrayresearch.forrester;

import com.deathrayresearch.forrester.event.EventHandler;
import com.deathrayresearch.forrester.event.SimulationEndEvent;
import com.deathrayresearch.forrester.event.SimulationStartEvent;
import com.deathrayresearch.forrester.event.TimestepEvent;
import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.TimeUnit;
import com.deathrayresearch.forrester.measure.Unit;
import com.deathrayresearch.forrester.measure.units.time.*;
import com.deathrayresearch.forrester.model.Model;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.model.Module;

import com.deathrayresearch.forrester.rate.Flow;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.EventBus;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Simulation is the execution environment for a model
 */
public class Simulation {

    private static final List<TimeUnit> SUPPORTED_TIMESTEPS = ImmutableList.of(
        Second.getInstance(),
        Minute.getInstance(),
        Hour.getInstance(),
        Day.getInstance(),
        Week.getInstance()
    );

    private static final String UNSUPPORTED_TIME_UNIT_MESSSAGE
        = "The provided time unit is not supported as a timeStep";

    private final Model model;

    private final Quantity duration;

    private final TimeUnit timeStep;

    private LocalDateTime currentDateTime;

    private Duration elapsedTime = Duration.ZERO;

    private final Set<EventHandler> eventHandlers = new HashSet<>();

    private final EventBus eventBus;

    public Simulation(Model model, TimeUnit timeStep, Quantity duration) {
        Preconditions.checkArgument(SUPPORTED_TIMESTEPS.contains(timeStep), UNSUPPORTED_TIME_UNIT_MESSSAGE);
        this.model = model;
        this.timeStep = timeStep;
        this.duration = duration;
        this.currentDateTime = LocalDateTime.now();
        eventBus = new EventBus();
    }

    public Simulation(Model model, TimeUnit timeStep, TimeUnit durationUnits, double durationAmount) {
        Preconditions.checkArgument(SUPPORTED_TIMESTEPS.contains(timeStep), UNSUPPORTED_TIME_UNIT_MESSSAGE);
        this.model = model;
        this.timeStep = timeStep;
        this.duration = new Quantity(durationAmount, durationUnits);
        this.currentDateTime = LocalDateTime.now();
        eventBus = new EventBus();
    }

    public Simulation(Model model, TimeUnit timeStep, Quantity duration, LocalDateTime startTime) {
        Preconditions.checkArgument(SUPPORTED_TIMESTEPS.contains(timeStep), UNSUPPORTED_TIME_UNIT_MESSSAGE);
        this.model = model;
        this.timeStep = timeStep;
        this.duration = duration;
        this.currentDateTime = startTime;
        eventBus = new EventBus();
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

        eventBus.post(new SimulationStartEvent(this));

        double durationInBaseUnits = duration.getUnit().ratioToBaseUnit();

        double totalSteps = (duration.getValue() * durationInBaseUnits) / (timeStep.ratioToBaseUnit());
        int step = 0;
        while (step < totalSteps) {
            HashMap<String, Quantity> flowMap = new HashMap<>();

            eventBus.post(new TimestepEvent(currentDateTime, model));
            List<Stock> stocks = model.getStocks();
            updateStocks(flowMap, stocks);
            for (Module module : model.getModules()) {
                stocks = module.getStocks();
                flowMap.clear();
                updateStocks(flowMap, stocks);
            }
            addStep(currentDateTime);
            step++;
        }

        eventBus.post(new SimulationEndEvent());
    }

    private void updateStocks(HashMap<String, Quantity> flowMap, List<Stock> stocks) {
        for (Stock stock : stocks) {
            Quantity qCurrent = stock.getCurrentValue();
            Set<Flow> stockInflows = stock.getInflows();
            handleFlows(true, flowMap, stock, qCurrent, stockInflows);
            Set<Flow> stockOutflows = stock.getOutflows();
            handleFlows(false, flowMap, stock, qCurrent, stockOutflows);
        }
    }

    private void handleFlows(boolean isInflow, HashMap<String, Quantity> flows, Stock stock, Quantity qCurrent, Set<Flow> flowSet) {
        for (Flow flow : flowSet) {
            Quantity q;
            if (flows.containsKey(flow.getName())) {
                q = flows.get(flow.getName());
            } else {
                q = flow.flowPerTimeUnit(timeStep);
                flows.put(flow.getName(), q);
            }
            if (isInflow) {
                qCurrent = qCurrent.add(q);
            } else {
                qCurrent = qCurrent.subtract(q);
            }
            stock.setValue(qCurrent.getValue());
        }
    }

    private void addStep(LocalDateTime dateTime) {
        String timeStepName = timeStep.getName();
        switch (timeStepName) {
            case "Second" :
                currentDateTime = dateTime.plusSeconds(1);
                elapsedTime = elapsedTime.plusSeconds(1);
                break;
            case "Minute" :
                currentDateTime = dateTime.plusMinutes(1);
                elapsedTime = elapsedTime.plusMinutes(1);
                break;
            case "Hour" :
                currentDateTime = dateTime.plusHours(1);
                elapsedTime = elapsedTime.plusHours(1);
                break;
            case "Day" :
                currentDateTime = dateTime.plusDays(1);
                elapsedTime = elapsedTime.plusDays(1);
                break;
            case "Week" :
                currentDateTime = dateTime.plusWeeks(1);
                elapsedTime = elapsedTime.plusDays(7);
                break;
        }
    }

    public Model getModel() {
        return model;
    }

    public Quantity getDuration() {
        return duration;
    }

    public Unit getTimeStep() {
        return timeStep;
    }

    public LocalDateTime getCurrentDateTime() {
        return currentDateTime;
    }

    public Duration getElapsedTime() {
        return elapsedTime;
    }
}

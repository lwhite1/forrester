package com.deathrayresearch.forrester;

import com.deathrayresearch.forrester.event.EventHandler;
import com.deathrayresearch.forrester.event.SimulationEndEvent;
import com.deathrayresearch.forrester.event.SimulationStartEvent;
import com.deathrayresearch.forrester.event.TimeStepEvent;
import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.TimeUnit;
import com.deathrayresearch.forrester.measure.Unit;
import com.deathrayresearch.forrester.model.Model;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.model.Module;
import com.deathrayresearch.forrester.model.Flow;
import com.deathrayresearch.forrester.model.Variable;
import com.google.common.eventbus.EventBus;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Simulation is the execution environment for a model
 */
public class Simulation {

    private final Model model;

    private final Quantity duration;

    private final TimeUnit timeStep;

    private int currentStep = 0;

    private LocalDateTime currentDateTime;

    private Duration elapsedTime = Duration.ZERO;

    private final Set<EventHandler> eventHandlers = new HashSet<>();

    private final EventBus eventBus;

    public Simulation(Model model, TimeUnit timeStep, Quantity duration) {
        this.model = model;
        this.timeStep = timeStep;
        this.duration = duration;
        this.currentDateTime = LocalDateTime.now();
        eventBus = new EventBus();
    }

    public Simulation(Model model, TimeUnit timeStep, TimeUnit durationUnits, double durationAmount) {
        this.model = model;
        this.timeStep = timeStep;
        this.duration = new Quantity(durationAmount, durationUnits);
        this.currentDateTime = LocalDateTime.now();
        eventBus = new EventBus();
    }

    public Simulation(Model model, TimeUnit timeStep, Quantity duration, LocalDateTime startTime) {
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

        while (currentStep <= totalSteps) {
            HashMap<String, Quantity> flowMap = new HashMap<>();

            eventBus.post(new TimeStepEvent(currentDateTime, model, currentStep, timeStep));
            recordVariableValues();
            List<Stock> stocks = model.getStocks();
            updateStocks(flowMap, stocks);
            for (Module module : model.getModules()) {
                stocks = module.getStocks();
                flowMap.clear();
                updateStocks(flowMap, stocks);
            }
            addStep(currentDateTime);
            currentStep++;
        }

        eventBus.post(new SimulationEndEvent());
    }

    private void updateStocks(HashMap<String, Quantity> flowMap, List<Stock> stocks) {
        for (Stock stock : stocks) {
            handleFlows(true, flowMap, stock, stock.getInflows());
            handleFlows(false, flowMap, stock, stock.getOutflows());
        }
    }

    private void recordVariableValues() {
        for (Variable variable : model.getVariables()) {
            variable.recordValue();
        }
        Collection<Variable> modelVariables = model.getVariables();
        for (Module module : model.getModules()) {
            for (Variable variable : module.getVariables()) {
                if (!modelVariables.contains(variable)) {
                    variable.recordValue();
                }
            }
        }
    }

    private void handleFlows(boolean isInflow, HashMap<String, Quantity> flows, Stock stock, Set<Flow> flowSet) {
        for (Flow flow : flowSet) {
            Quantity q;
            if (flows.containsKey(flow.getName())) {
                q = flows.get(flow.getName());
            } else {
                q = flow.flowPerTimeUnit(timeStep);
                flows.put(flow.getName(), q);
            }
            flow.recordValue(q);

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
        long seconds = (long) timeStep.ratioToBaseUnit();
        currentDateTime = dateTime.plusSeconds(seconds);
        elapsedTime = elapsedTime.plusSeconds(seconds);
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

    public int getCurrentStep() {
        return currentStep;
    }
}

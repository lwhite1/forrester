package com.deathrayresearch.forrester;

import com.deathrayresearch.forrester.event.EventHandler;
import com.deathrayresearch.forrester.event.SimulationEndEvent;
import com.deathrayresearch.forrester.event.SimulationStartEvent;
import com.deathrayresearch.forrester.event.TimestepEvent;
import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.TimeUnit;
import com.deathrayresearch.forrester.measure.Unit;
import com.deathrayresearch.forrester.model.Flow;
import com.deathrayresearch.forrester.model.Model;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.model.SubSystem;

import com.google.common.eventbus.EventBus;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 *
 */
public class Simulation {

    private final Model model;

    private final Quantity duration;

    private final TimeUnit timeStep;

    private LocalDateTime currentDateTime;

    private final Set<EventHandler> eventHandlers = new HashSet<>();

    private final EventBus eventBus;

    public Simulation(Model model, TimeUnit timeStep, Quantity duration) {
        this.model = model;
        this.timeStep = timeStep;
        this.duration = duration;
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
        eventHandlers.add(handler);
    }

    public void removeEventHandler(EventHandler handler) {
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
            HashMap<String, Quantity> flows = new HashMap<>();

            eventBus.post(new TimestepEvent(currentDateTime, model));

            for (Stock stock : model.getStocks()) {

                Quantity qCurrent = stock.getCurrentValue();
                System.out.println(stock.getName() + " : " + qCurrent);

                Set<Flow> stockInflows = stock.getInflows();
                for (Flow inflow : stockInflows) {
                    Quantity q;
                    if (flows.containsKey(inflow.getName())) {
                        q = flows.get(inflow.getName());
                    } else {
                        q = inflow.getRate().flowPerTimeUnit(timeStep);
                        flows.put(inflow.getName(), q);
                    }
                    qCurrent = qCurrent.add(q);
                    stock.setCurrentValue(qCurrent);
                }
                Set<Flow> stockOutflows = stock.getOutflows();
                for (Flow outflow : stockOutflows) {
                    Quantity q;
                    if (flows.containsKey(outflow.getName())) {
                        q = flows.get(outflow.getName());
                    } else {
                        q = outflow.getRate().flowPerTimeUnit(timeStep);
                        flows.put(outflow.getName(), q);
                    }
                    qCurrent = qCurrent.subtract(q);
                    stock.setCurrentValue(qCurrent);
                }
            }

            addStep(currentDateTime);
            step++;
        }

        eventBus.post(new SimulationEndEvent());
    }
    public void executeSubSystems() {

        eventBus.post(new SimulationStartEvent(this));

        double durationInBaseUnits = duration.getUnit().ratioToBaseUnit();

        double totalSteps = (duration.getValue() * durationInBaseUnits) / (timeStep.ratioToBaseUnit());
        int step = 0;
        while (step < totalSteps) {
            HashMap<String, Quantity> flows = new HashMap<>();

            eventBus.post(new TimestepEvent(currentDateTime, model));
            for (SubSystem subSystem : model.getSubSystems()) {
                for (Stock stock : subSystem.getStocks()) {

                    Quantity qCurrent = stock.getCurrentValue();
                    System.out.println(stock.getName() + " : " + qCurrent);

                    Set<Flow> stockInflows = stock.getInflows();
                    for (Flow inflow : stockInflows) {
                        Quantity q;
                        if (flows.containsKey(inflow.getName())) {
                            q = flows.get(inflow.getName());
                        } else {
                            q = inflow.getRate().flowPerTimeUnit(timeStep);
                            flows.put(inflow.getName(), q);
                        }
                        qCurrent = qCurrent.add(q);
                        stock.setCurrentValue(qCurrent);
                    }
                    Set<Flow> stockOutflows = stock.getOutflows();
                    for (Flow outflow : stockOutflows) {
                        Quantity q;
                        if (flows.containsKey(outflow.getName())) {
                            q = flows.get(outflow.getName());
                        } else {
                            q = outflow.getRate().flowPerTimeUnit(timeStep);
                            flows.put(outflow.getName(), q);
                        }
                        qCurrent = qCurrent.subtract(q);
                        stock.setCurrentValue(qCurrent);
                    }
                }
            }
            addStep(currentDateTime);
            step++;
        }

        eventBus.post(new SimulationEndEvent());
    }

    private void addStep(LocalDateTime dateTime) {
        String timeStepName = timeStep.getName();
        switch (timeStepName) {
            case "Second" :
                currentDateTime = dateTime.plusSeconds(1);
                break;
            case "Minute" :
                currentDateTime = dateTime.plusMinutes(1);
                break;
            case "Hour" :
                currentDateTime = dateTime.plusHours(1);
                break;
            case "Day" :
                currentDateTime = dateTime.plusDays(1);
                break;
            case "Week" :
                currentDateTime = dateTime.plusWeeks(1);
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
}

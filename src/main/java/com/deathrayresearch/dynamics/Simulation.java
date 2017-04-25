package com.deathrayresearch.dynamics;

import com.deathrayresearch.dynamics.event.CsvSubscriber;
import com.deathrayresearch.dynamics.event.SimulationEndEvent;
import com.deathrayresearch.dynamics.event.SimulationStartEvent;
import com.deathrayresearch.dynamics.event.TimestepEvent;
import com.deathrayresearch.dynamics.measure.Dimension;
import com.deathrayresearch.dynamics.measure.Quantity;
import com.deathrayresearch.dynamics.measure.Unit;
import com.deathrayresearch.dynamics.measure.dimension.Time;
import com.deathrayresearch.dynamics.model.Flow;
import com.deathrayresearch.dynamics.model.Model;
import com.deathrayresearch.dynamics.model.Stock;
import com.deathrayresearch.dynamics.ui.ChartViewer;
import com.google.common.eventbus.EventBus;

import java.time.LocalDateTime;
import java.util.Set;

/**
 *
 */
public class Simulation {

    private final Model model;

    private final Quantity<Time> duration;

    private final Unit<Time> timeStep;

    private LocalDateTime currentDateTime;

    public Simulation(Model model, Unit<Time> timeStep, Quantity<Time> duration) {
        this.model = model;
        this.timeStep = timeStep;
        this.duration = duration;
        this.currentDateTime = LocalDateTime.now();
    }

    public Simulation(Model model, Unit<Time> timeStep, Quantity<Time> duration, LocalDateTime startTime) {
        this.model = model;
        this.timeStep = timeStep;
        this.duration = duration;
        this.currentDateTime = startTime;
    }

    public void execute() {

        EventBus eventBus = new EventBus();
        CsvSubscriber.newInstance(eventBus, "run1.out.csv");
        ChartViewer.newInstance(eventBus);

        eventBus.post(new SimulationStartEvent(this));

        double durationInBaseUnits = duration.getUnit().ratioToBaseUnit();

        double totalSteps =  (duration.getValue() * durationInBaseUnits) / (timeStep.ratioToBaseUnit());
        int step = 0;

        while (step < totalSteps) {
            eventBus.post(new TimestepEvent(currentDateTime, model));
            for (Stock<Dimension> stock : model.getStocks()) {
                Quantity<Dimension> qCurrent = stock.getCurrentValue();
                Set<Flow<Dimension>> stockInflows = stock.getInflows();
                for (Flow inflow : stockInflows) {
                    Quantity<Dimension> q = inflow.getRate().flowPerTimeUnit(timeStep);
                    qCurrent = qCurrent.add(q);
                    stock.setCurrentValue(qCurrent);
                }
                Set<Flow<Dimension>> stockOutflows = stock.getOutflows();
                for (Flow outflow : stockOutflows) {
                    Quantity q = outflow.getRate().flowPerTimeUnit(timeStep);
                    qCurrent = qCurrent.subtract(q);
                    stock.setCurrentValue(qCurrent);
                }
                addStep(currentDateTime);
                System.out.println(stock.getName() + " : " + qCurrent);
            }
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

    public Quantity<Time> getDuration() {
        return duration;
    }

    public Unit<Time> getTimeStep() {
        return timeStep;
    }

    public LocalDateTime getCurrentDateTime() {
        return currentDateTime;
    }
}

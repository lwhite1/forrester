package com.deathrayresearch.forrester.event;

import com.deathrayresearch.forrester.measure.TimeUnit;
import com.deathrayresearch.forrester.model.Model;

import java.time.LocalDateTime;

/**
 *
 */
public class TimeStepEvent {

    private final LocalDateTime currentTime;
    private final int step;
    private final Model model;
    private final TimeUnit timeStep;

    public TimeStepEvent(LocalDateTime currentTime, Model model, int step, TimeUnit timeStep) {
        this.currentTime = currentTime;
        this.model = model;
        this.step = step;
        this.timeStep = timeStep;
    }

    public LocalDateTime getCurrentTime() {
        return currentTime;
    }

    public int getStep() {
        return step;
    }

    public TimeUnit getTimeStep() {
        return timeStep;
    }

    public Model getModel() {
        return model;
    }
}

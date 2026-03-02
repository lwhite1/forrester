package com.deathrayresearch.forrester.event;

import com.deathrayresearch.forrester.measure.TimeUnit;
import com.deathrayresearch.forrester.model.Model;

import java.time.LocalDateTime;

/**
 * Event fired at each time step of a simulation, <em>before</em> stocks are updated
 * for that step. Handlers observe stock values from the previous step (or initial values
 * at step 0). Contains the simulation clock, model reference, and step metadata.
 */
public class TimeStepEvent {

    private final LocalDateTime currentTime;
    private final int step;
    private final Model model;
    private final TimeUnit timeStep;

    /**
     * Creates a new time step event.
     *
     * @param currentTime the simulation clock time after this step
     * @param model       the model whose state was updated
     * @param step        the zero-based step index
     * @param timeStep    the time unit duration of each step
     */
    public TimeStepEvent(LocalDateTime currentTime, Model model, int step, TimeUnit timeStep) {
        this.currentTime = currentTime;
        this.model = model;
        this.step = step;
        this.timeStep = timeStep;
    }

    /**
     * Returns the simulation clock time after this step.
     */
    public LocalDateTime getCurrentTime() {
        return currentTime;
    }

    /**
     * Returns the zero-based index of this time step.
     */
    public int getStep() {
        return step;
    }

    /**
     * Returns the time unit duration of each step.
     */
    public TimeUnit getTimeStep() {
        return timeStep;
    }

    /**
     * Returns the model whose state was updated in this step.
     */
    public Model getModel() {
        return model;
    }
}

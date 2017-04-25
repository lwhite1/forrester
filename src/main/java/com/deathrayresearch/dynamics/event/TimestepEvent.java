package com.deathrayresearch.dynamics.event;

import com.deathrayresearch.dynamics.model.Model;

import java.time.LocalDateTime;

/**
 *
 */
public class TimestepEvent {

    private LocalDateTime currentTime;
    private Model model;

    public TimestepEvent(LocalDateTime currentTime, Model model) {
        this.currentTime = currentTime;
        this.model = model;
    }

    public LocalDateTime getCurrentTime() {
        return currentTime;
    }

    public Model getModel() {
        return model;
    }
}

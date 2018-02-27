package com.deathrayresearch.forrester.event;

/**
 *
 */
public interface EventHandler {

    void handleTimeStepEvent(TimestepEvent event);

    void handleSimulationStartEvent(SimulationStartEvent event);

    void handleSimulationEndEvent(SimulationEndEvent event);
}

package com.deathrayresearch.forrester.event;

/**
 *
 */
public interface EventHandler {

    void handleTimeStepEvent(TimeStepEvent event);

    void handleSimulationStartEvent(SimulationStartEvent event);

    void handleSimulationEndEvent(SimulationEndEvent event);
}

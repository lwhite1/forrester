package com.deathrayresearch.forrester.event;

/**
 *
 */
public interface EventHandler {

    void handleTimestepEvent(TimestepEvent event);

    void handleSimulationStartEvent(SimulationStartEvent event);

    void handleSimulationEndEvent(SimulationEndEvent event);
}

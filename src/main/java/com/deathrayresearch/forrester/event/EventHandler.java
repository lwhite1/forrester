package com.deathrayresearch.forrester.event;

/**
 * Listener interface for receiving simulation lifecycle events.
 * Implementations handle time step progression, simulation start, and simulation end notifications.
 */
public interface EventHandler {

    /**
     * Called after each time step in the simulation has been computed.
     *
     * @param event the time step event containing the current model state
     */
    void handleTimeStepEvent(TimeStepEvent event);

    /**
     * Called once when the simulation begins, before any time steps are computed.
     *
     * @param event the start event containing the simulation and model references
     */
    void handleSimulationStartEvent(SimulationStartEvent event);

    /**
     * Called once when the simulation has completed all time steps.
     *
     * @param event the end event
     */
    void handleSimulationEndEvent(SimulationEndEvent event);
}

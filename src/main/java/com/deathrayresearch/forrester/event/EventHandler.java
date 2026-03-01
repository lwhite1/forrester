package com.deathrayresearch.forrester.event;

import com.google.common.eventbus.Subscribe;

/**
 * Listener interface for receiving simulation lifecycle events.
 * Implementations handle time step progression, simulation start, and simulation end notifications.
 *
 * <p>All methods have default no-op implementations annotated with {@code @Subscribe} so that
 * Guava EventBus delivers events correctly. Implementors only need to override the methods
 * they care about — there is no need to manually add {@code @Subscribe} when overriding,
 * because EventBus inherits the annotation from the default method.
 */
public interface EventHandler {

    /**
     * Called after each time step in the simulation has been computed.
     *
     * @param event the time step event containing the current model state
     */
    @Subscribe
    default void handleTimeStepEvent(TimeStepEvent event) {
        // no-op by default
    }

    /**
     * Called once when the simulation begins, before any time steps are computed.
     *
     * @param event the start event containing the simulation and model references
     */
    @Subscribe
    default void handleSimulationStartEvent(SimulationStartEvent event) {
        // no-op by default
    }

    /**
     * Called once when the simulation has completed all time steps.
     *
     * @param event the end event
     */
    @Subscribe
    default void handleSimulationEndEvent(SimulationEndEvent event) {
        // no-op by default
    }
}

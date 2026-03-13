package systems.courant.sd.event;

/**
 * Listener interface for receiving simulation lifecycle events.
 * Implementations handle time step progression, simulation start, and simulation end notifications.
 *
 * <p>Implementors only need to override the methods they care about.
 */
public interface EventHandler {

    /**
     * Called after each time step in the simulation has been computed.
     *
     * @param event the time step event containing the current model state
     */
    default void handleTimeStepEvent(TimeStepEvent event) {
        // no-op by default
    }

    /**
     * Called once when the simulation begins, before any time steps are computed.
     *
     * @param event the start event containing the simulation and model references
     */
    default void handleSimulationStartEvent(SimulationStartEvent event) {
        // no-op by default
    }

    /**
     * Called once when the simulation has completed all time steps.
     *
     * @param event the end event
     */
    default void handleSimulationEndEvent(SimulationEndEvent event) {
        // no-op by default
    }
}

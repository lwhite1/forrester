package systems.courant.sd.event;

import systems.courant.sd.model.Model;

/**
 * Event fired when a simulation run has completed all of its time steps.
 */
public class SimulationEndEvent {

    private final Model model;

    /**
     * Creates an end event with no model reference (for backwards compatibility).
     */
    public SimulationEndEvent() {
        this.model = null;
    }

    /**
     * Creates an end event for the given model.
     *
     * @param model the model that was simulated
     */
    public SimulationEndEvent(Model model) {
        this.model = model;
    }

    /**
     * Returns the model that was simulated, or {@code null} if not available.
     */
    public Model getModel() {
        return model;
    }
}

package com.deathrayresearch.forrester.event;


import com.deathrayresearch.forrester.Simulation;
import com.deathrayresearch.forrester.model.Model;

/**
 * Event fired when a simulation run begins, before any time steps are computed.
 */
public class SimulationStartEvent {

    private final Simulation simulation;

    /**
     * Creates a new start event for the given simulation.
     *
     * @param simulation the simulation that is starting
     */
    public SimulationStartEvent(Simulation simulation) {
        this.simulation = simulation;
    }

    /**
     * Returns the model being simulated.
     */
    public Model getModel() {
        return simulation.getModel();
    }

    /**
     * Returns the simulation that is starting.
     */
    public Simulation getSimulation() {
        return simulation;
    }
}

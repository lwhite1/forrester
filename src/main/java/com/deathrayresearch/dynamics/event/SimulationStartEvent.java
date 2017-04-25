package com.deathrayresearch.dynamics.event;

import com.deathrayresearch.dynamics.Simulation;
import com.deathrayresearch.dynamics.model.Model;

/**
 *
 */
public class SimulationStartEvent {

    private final Simulation simulation;

    public SimulationStartEvent(Simulation simulation) {
        this.simulation = simulation;
    }

    public Model getModel() {
        return simulation.getModel();
    }

    public Simulation getSimulation() {
        return simulation;
    }
}

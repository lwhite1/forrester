package com.deathrayresearch.forrester.event;


import com.deathrayresearch.forrester.Simulation;
import com.deathrayresearch.forrester.model.Model;

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

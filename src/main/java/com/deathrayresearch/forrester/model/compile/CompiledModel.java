package com.deathrayresearch.forrester.model.compile;

import com.deathrayresearch.forrester.Simulation;
import com.deathrayresearch.forrester.event.EventHandler;
import com.deathrayresearch.forrester.event.TimeStepEvent;
import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.TimeUnit;
import com.deathrayresearch.forrester.measure.UnitRegistry;
import com.deathrayresearch.forrester.model.Model;
import com.deathrayresearch.forrester.model.def.ModelDefinition;
import com.deathrayresearch.forrester.model.def.SimulationSettings;
import com.google.common.eventbus.Subscribe;

import java.util.List;

/**
 * The result of compiling a {@link ModelDefinition}. Contains the runnable model,
 * stateful formulas that need resetting between runs, and a reference to the source definition.
 */
public class CompiledModel {

    private final Model model;
    private final List<Resettable> resettables;
    private final ModelDefinition source;
    private final int[] stepHolder;

    public CompiledModel(Model model, List<Resettable> resettables, ModelDefinition source,
                         int[] stepHolder) {
        this.model = model;
        this.resettables = List.copyOf(resettables);
        this.source = source;
        this.stepHolder = stepHolder;
    }

    public Model getModel() {
        return model;
    }

    public List<Resettable> getResettables() {
        return resettables;
    }

    public ModelDefinition getSource() {
        return source;
    }

    /**
     * Creates a simulation from the compiled model and the given time step and duration.
     * Installs an event handler that keeps the compiled model's step counter in sync.
     */
    public Simulation createSimulation(TimeUnit timeStep, double duration, TimeUnit durationUnit) {
        Simulation sim = new Simulation(model, timeStep, durationUnit, duration);
        installStepSync(sim);
        return sim;
    }

    /**
     * Creates a simulation using the default simulation settings from the source definition.
     *
     * @throws IllegalStateException if no default simulation settings are defined
     */
    public Simulation createSimulation() {
        SimulationSettings settings = source.defaultSimulation();
        if (settings == null) {
            throw new IllegalStateException("No default simulation settings defined");
        }
        UnitRegistry registry = new UnitRegistry();
        TimeUnit timeStep = registry.resolveTimeUnit(settings.timeStep());
        TimeUnit durationUnit = registry.resolveTimeUnit(settings.durationUnit());
        Simulation sim = new Simulation(model, timeStep, new Quantity(settings.duration(), durationUnit));
        installStepSync(sim);
        return sim;
    }

    /**
     * Resets all stateful formulas so the model can be re-simulated.
     */
    public void reset() {
        stepHolder[0] = 0;
        for (Resettable r : resettables) {
            r.reset();
        }
    }

    private void installStepSync(Simulation sim) {
        sim.addEventHandler(new StepSyncHandler(stepHolder, sim));
    }

    /**
     * Event handler that synchronizes the compiled model's step counter with the simulation.
     */
    private static class StepSyncHandler implements EventHandler {
        private final int[] stepHolder;
        private final Simulation sim;

        StepSyncHandler(int[] stepHolder, Simulation sim) {
            this.stepHolder = stepHolder;
            this.sim = sim;
        }

        @Subscribe
        public void onTimeStep(TimeStepEvent event) {
            stepHolder[0] = sim.getCurrentStep();
        }
    }
}

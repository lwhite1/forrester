package com.deathrayresearch.forrester.model.compile;

import com.deathrayresearch.forrester.Simulation;
import com.deathrayresearch.forrester.event.EventHandler;
import com.deathrayresearch.forrester.event.TimeStepEvent;
import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.TimeUnit;
import com.deathrayresearch.forrester.measure.UnitRegistry;
import com.deathrayresearch.forrester.model.Model;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.model.def.ModelDefinition;
import com.deathrayresearch.forrester.model.def.SimulationSettings;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The result of compiling a {@link ModelDefinition}. Contains the runnable model,
 * stateful formulas that need resetting between runs, and a reference to the source definition.
 */
public class CompiledModel {

    private final Model model;
    private final List<Resettable> resettables;
    private final ModelDefinition source;
    private final int[] stepHolder;
    private final double[] dtHolder;
    private final TimeUnit[] simTimeUnitHolder;
    private final UnitRegistry unitRegistry;
    private final Map<Stock, Double> initialStockValues;

    /**
     * Creates a compiled model wrapping the runnable model and its compilation artifacts.
     *
     * @param model       the compiled runnable model
     * @param resettables stateful formulas that need resetting between simulation runs
     * @param source      the original model definition
     * @param stepHolder  a single-element array tracking the current simulation step
     * @param dtHolder    a single-element array holding the DT value
     * @param simTimeUnitHolder a single-element array holding the simulation time unit
     * @param unitRegistry the unit registry used during compilation
     */
    public CompiledModel(Model model, List<Resettable> resettables, ModelDefinition source,
                         int[] stepHolder, double[] dtHolder, TimeUnit[] simTimeUnitHolder,
                         UnitRegistry unitRegistry) {
        this.model = model;
        this.resettables = List.copyOf(resettables);
        this.source = source;
        this.stepHolder = stepHolder;
        this.dtHolder = dtHolder;
        this.simTimeUnitHolder = simTimeUnitHolder;
        this.unitRegistry = unitRegistry;
        this.initialStockValues = new LinkedHashMap<>();
        for (Stock stock : model.getStocks()) {
            initialStockValues.put(stock, stock.getValue());
        }
    }

    /**
     * Returns the compiled runnable model.
     */
    public Model getModel() {
        return model;
    }

    /**
     * Returns the list of stateful formulas that are reset between simulation runs.
     */
    public List<Resettable> getResettables() {
        return resettables;
    }

    /**
     * Returns the original model definition from which this compiled model was produced.
     */
    public ModelDefinition getSource() {
        return source;
    }

    /**
     * Sets the DT (integration time step) value used by formulas that reference DT.
     * Defaults to 1.0 (one simulation step = one time unit).
     *
     * @param dt the time step value
     */
    public void setDt(double dt) {
        dtHolder[0] = dt;
    }

    /**
     * Returns the current DT value.
     */
    public double getDt() {
        return dtHolder[0];
    }

    /**
     * Creates a simulation from the compiled model and the given time step and duration.
     * Installs an event handler that keeps the compiled model's step counter in sync.
     */
    public Simulation createSimulation(TimeUnit timeStep, double duration, TimeUnit durationUnit) {
        simTimeUnitHolder[0] = timeStep;
        Simulation sim = new Simulation(model, timeStep, durationUnit, duration);
        installStepSync(sim);
        return sim;
    }

    /**
     * Creates a simulation from the compiled model with the given time step and duration.
     * Installs an event handler that keeps the compiled model's step counter in sync.
     */
    public Simulation createSimulation(TimeUnit timeStep, Quantity duration) {
        simTimeUnitHolder[0] = timeStep;
        Simulation sim = new Simulation(model, timeStep, duration);
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
        TimeUnit timeStep = unitRegistry.resolveTimeUnit(settings.timeStep());
        TimeUnit durationUnit = unitRegistry.resolveTimeUnit(settings.durationUnit());
        simTimeUnitHolder[0] = timeStep;
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
        for (Map.Entry<Stock, Double> entry : initialStockValues.entrySet()) {
            entry.getKey().setValue(entry.getValue());
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

        @Override
        public void handleTimeStepEvent(TimeStepEvent event) {
            stepHolder[0] = sim.getCurrentStep();
        }
    }
}

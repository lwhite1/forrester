package systems.courant.sd.model.compile;

import systems.courant.sd.Simulation;
import systems.courant.sd.event.EventHandler;
import systems.courant.sd.event.TimeStepEvent;
import systems.courant.sd.measure.Quantity;
import systems.courant.sd.measure.TimeUnit;
import systems.courant.sd.measure.UnitRegistry;
import systems.courant.sd.model.Flow;
import systems.courant.sd.model.Model;
import systems.courant.sd.model.Module;
import systems.courant.sd.model.Stock;
import systems.courant.sd.model.Variable;
import systems.courant.sd.model.def.ModelDefinition;
import systems.courant.sd.model.def.SimulationSettings;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * The result of compiling a {@link ModelDefinition}. Contains the runnable model,
 * stateful formulas that need resetting between runs, and a reference to the source definition.
 */
public class CompiledModel {

    private final Model model;
    private final List<Resettable> resettables;
    private final ModelDefinition source;
    private final long[] stepHolder;
    private final double[] dtHolder;
    private final TimeUnit[] simTimeUnitHolder;
    private final UnitRegistry unitRegistry;
    private final List<String> compilationWarnings;

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
                         long[] stepHolder, double[] dtHolder, TimeUnit[] simTimeUnitHolder,
                         UnitRegistry unitRegistry) {
        this(model, resettables, source, stepHolder, dtHolder, simTimeUnitHolder,
                unitRegistry, List.of());
    }

    /**
     * Creates a compiled model with compilation warnings.
     *
     * @param model       the compiled runnable model
     * @param resettables stateful formulas that need resetting between simulation runs
     * @param source      the original model definition
     * @param stepHolder  a single-element array tracking the current simulation step
     * @param dtHolder    a single-element array holding the DT value
     * @param simTimeUnitHolder a single-element array holding the simulation time unit
     * @param unitRegistry the unit registry used during compilation
     * @param compilationWarnings non-fatal warnings from compilation
     */
    public CompiledModel(Model model, List<Resettable> resettables, ModelDefinition source,
                         long[] stepHolder, double[] dtHolder, TimeUnit[] simTimeUnitHolder,
                         UnitRegistry unitRegistry, List<String> compilationWarnings) {
        this.model = model;
        this.resettables = List.copyOf(resettables);
        this.source = source;
        this.stepHolder = stepHolder;
        this.dtHolder = dtHolder;
        this.simTimeUnitHolder = simTimeUnitHolder;
        this.unitRegistry = unitRegistry;
        this.compilationWarnings = List.copyOf(compilationWarnings);
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
     * Returns non-fatal warnings from compilation (e.g. delay parameters that
     * could not be resolved at compile time and fell back to defaults).
     */
    public List<String> getCompilationWarnings() {
        return compilationWarnings;
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
        sim.setDt(dtHolder[0]);
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
        sim.setDt(dtHolder[0]);
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
        setDt(settings.dt());
        Simulation sim = new Simulation(model, timeStep, new Quantity(settings.duration(), durationUnit));
        sim.setStrictMode(settings.strictMode());
        sim.setSavePer(settings.savePer());
        sim.setDt(settings.dt());
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
        for (Stock stock : model.getStocks()) {
            stock.resetToInitialValue();
        }
        for (Module module : model.getModules()) {
            resetModuleStocks(module);
        }
        // Clear history for top-level flows and variables, then walk modules
        Set<Flow> seenFlows = Collections.newSetFromMap(new IdentityHashMap<>());
        Set<Variable> seenVars = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Flow flow : model.getFlows()) {
            seenFlows.add(flow);
            flow.clearHistory();
        }
        for (Variable variable : model.getVariables()) {
            seenVars.add(variable);
            variable.clearHistory();
        }
        for (Module module : model.getModules()) {
            clearModuleHistory(module, seenFlows, seenVars);
        }
    }

    private static void clearModuleHistory(Module module, Set<Flow> seenFlows,
                                           Set<Variable> seenVars) {
        for (Flow flow : module.getFlows()) {
            if (seenFlows.add(flow)) {
                flow.clearHistory();
            }
        }
        for (Variable variable : module.getVariables()) {
            if (seenVars.add(variable)) {
                variable.clearHistory();
            }
        }
        for (Module child : module.getSubModules().values()) {
            clearModuleHistory(child, seenFlows, seenVars);
        }
    }

    private static void resetModuleStocks(Module module) {
        for (Stock stock : module.getStocks()) {
            stock.resetToInitialValue();
        }
        for (Module child : module.getSubModules().values()) {
            resetModuleStocks(child);
        }
    }

    private void installStepSync(Simulation sim) {
        sim.addEventHandler(new StepSyncHandler(stepHolder, sim));
    }

    /**
     * Event handler that synchronizes the compiled model's step counter with the simulation.
     */
    private static class StepSyncHandler implements EventHandler {
        private final long[] stepHolder;
        private final Simulation sim;

        StepSyncHandler(long[] stepHolder, Simulation sim) {
            this.stepHolder = stepHolder;
            this.sim = sim;
        }

        @Override
        public void handleTimeStepEvent(TimeStepEvent event) {
            stepHolder[0] = sim.getCurrentStep();
        }
    }
}

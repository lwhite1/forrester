package systems.courant.sd.sweep;

import systems.courant.sd.Simulation;
import systems.courant.sd.measure.Quantity;
import systems.courant.sd.measure.TimeUnit;
import systems.courant.sd.model.Model;
import systems.courant.sd.model.compile.CompiledModel;

import java.util.Map;
import java.util.function.DoubleFunction;
import java.util.function.Function;

/**
 * Eliminates the duplicated model-factory → compile → simulate → collect pattern
 * found across sweep classes ({@link MonteCarlo}, {@link MultiParameterSweep},
 * {@link Optimizer}, {@link ParameterSweep}, {@link ExtremeConditionTest}).
 *
 * <p>Provides three levels of control:
 * <ul>
 *   <li>{@link #run} — full pipeline: create simulation, attach handler, execute, return result
 *   <li>{@link #createSimulation} — just the factory branching (caller customizes before executing)
 *   <li>{@link #execute} — attach handler + execute + return result (after caller customization)
 * </ul>
 */
public final class SimulationRunner {

    private SimulationRunner() {
    }

    /**
     * Full pipeline for multi-parameter factories: creates a simulation from whichever factory
     * is non-null, attaches a {@link RunResult} handler, executes, and returns the result.
     *
     * @param modelFactory         factory that builds a {@link Model} from parameter values (may be null)
     * @param compiledModelFactory factory that builds a {@link CompiledModel} from parameter values (may be null)
     * @param paramMap             the parameter values for this run
     * @param timeStep             the simulation time step
     * @param duration             the simulation duration
     * @return the populated {@link RunResult}
     */
    public static RunResult run(Function<Map<String, Double>, Model> modelFactory,
                                Function<Map<String, Double>, CompiledModel> compiledModelFactory,
                                Map<String, Double> paramMap,
                                TimeUnit timeStep, Quantity duration) {
        Simulation simulation = createSimulation(modelFactory, compiledModelFactory,
                paramMap, timeStep, duration);
        return execute(simulation, paramMap);
    }

    /**
     * Full pipeline for single-parameter factories: creates a simulation from whichever factory
     * is non-null, attaches a {@link RunResult} handler, executes, and returns the result.
     *
     * @param modelFactory         factory that builds a {@link Model} from a parameter value (may be null)
     * @param compiledModelFactory factory that builds a {@link CompiledModel} from a parameter value (may be null)
     * @param parameterName        the name of the swept parameter
     * @param value                the parameter value for this run
     * @param timeStep             the simulation time step
     * @param duration             the simulation duration
     * @return the populated {@link RunResult}
     */
    public static RunResult run(DoubleFunction<Model> modelFactory,
                                DoubleFunction<CompiledModel> compiledModelFactory,
                                String parameterName, double value,
                                TimeUnit timeStep, Quantity duration) {
        Simulation simulation;
        if (compiledModelFactory != null) {
            CompiledModel compiled = compiledModelFactory.apply(value);
            simulation = compiled.createSimulation(timeStep, duration);
        } else {
            Model model = modelFactory.apply(value);
            simulation = new Simulation(model, timeStep, duration);
        }
        return execute(simulation, Map.of(parameterName, value));
    }

    /**
     * Creates a {@link Simulation} from whichever factory is non-null, without executing it.
     * Use this when the caller needs to customize the simulation (e.g. {@code setStrictMode})
     * before execution.
     *
     * @param modelFactory         factory that builds a {@link Model} from parameter values (may be null)
     * @param compiledModelFactory factory that builds a {@link CompiledModel} from parameter values (may be null)
     * @param paramMap             the parameter values for this run
     * @param timeStep             the simulation time step
     * @param duration             the simulation duration
     * @return the configured but not-yet-executed {@link Simulation}
     */
    public static Simulation createSimulation(
            Function<Map<String, Double>, Model> modelFactory,
            Function<Map<String, Double>, CompiledModel> compiledModelFactory,
            Map<String, Double> paramMap,
            TimeUnit timeStep, Quantity duration) {
        if (compiledModelFactory != null) {
            CompiledModel compiled = compiledModelFactory.apply(paramMap);
            return compiled.createSimulation(timeStep, duration);
        } else {
            Model model = modelFactory.apply(paramMap);
            return new Simulation(model, timeStep, duration);
        }
    }

    /**
     * Attaches a {@link RunResult} handler to the simulation, executes it, and returns
     * the populated result. Use after {@link #createSimulation} when the caller needs
     * to customize the simulation before running.
     *
     * @param simulation the simulation to execute
     * @param paramMap   the parameter values to record in the result
     * @return the populated {@link RunResult}
     */
    public static RunResult execute(Simulation simulation, Map<String, Double> paramMap) {
        RunResult runResult = new RunResult(paramMap);
        simulation.addEventHandler(runResult);
        simulation.execute();
        return runResult;
    }
}

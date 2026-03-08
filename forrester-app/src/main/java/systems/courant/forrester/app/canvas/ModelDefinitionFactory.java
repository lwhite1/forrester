package systems.courant.forrester.app.canvas;

import systems.courant.forrester.measure.Quantity;
import systems.courant.forrester.measure.TimeUnit;
import systems.courant.forrester.measure.UnitRegistry;
import systems.courant.forrester.model.compile.CompiledModel;
import systems.courant.forrester.model.compile.ModelCompiler;
import systems.courant.forrester.model.def.ConstantDef;
import systems.courant.forrester.model.def.ModelDefinition;
import systems.courant.forrester.model.def.SimulationSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Bridges {@link ModelDefinition} to the engine's analysis APIs by creating
 * model factory functions that compile fresh models with overridden constants.
 */
public final class ModelDefinitionFactory {

    private ModelDefinitionFactory() {
    }

    /**
     * Creates a factory function that accepts a map of constant name to value,
     * applies overrides to the definition, compiles, and returns the compiled model.
     * Returning {@link CompiledModel} (rather than bare {@link Model}) ensures that
     * sweep classes can create simulations with proper step synchronization,
     * which is required for time-dependent functions (STEP, RAMP, PULSE, etc.).
     */
    public static Function<Map<String, Double>, CompiledModel> createFactory(
            ModelDefinition def, SimulationSettings settings) {
        return paramMap -> {
            ModelDefinition overridden = applyConstantOverrides(def, paramMap);
            ModelDefinition withSettings = embedSettings(overridden, settings);
            ModelCompiler compiler = new ModelCompiler();
            return compiler.compile(withSettings);
        };
    }

    /**
     * Creates a single-parameter factory for use with {@link systems.courant.forrester.sweep.ParameterSweep}.
     * Returns {@link CompiledModel} to ensure proper step synchronization.
     */
    public static java.util.function.DoubleFunction<CompiledModel> createSingleParamFactory(
            ModelDefinition def, SimulationSettings settings, String paramName) {
        return value -> {
            ModelDefinition overridden = applyConstantOverrides(def, Map.of(paramName, value));
            ModelDefinition withSettings = embedSettings(overridden, settings);
            ModelCompiler compiler = new ModelCompiler();
            return compiler.compile(withSettings);
        };
    }

    /**
     * Resolves the time step unit from simulation settings.
     */
    public static TimeUnit resolveTimeStep(SimulationSettings settings) {
        UnitRegistry registry = new UnitRegistry();
        return registry.resolveTimeUnit(settings.timeStep());
    }

    /**
     * Resolves the simulation duration as a {@link Quantity}.
     */
    public static Quantity resolveDuration(SimulationSettings settings) {
        UnitRegistry registry = new UnitRegistry();
        TimeUnit durationUnit = registry.resolveTimeUnit(settings.durationUnit());
        return new Quantity(settings.duration(), durationUnit);
    }

    /**
     * Creates a new ModelDefinition with modified constant values.
     */
    public static ModelDefinition applyConstantOverrides(
            ModelDefinition def, Map<String, Double> overrides) {
        if (overrides.isEmpty()) {
            return def;
        }
        List<ConstantDef> updatedConstants = new ArrayList<>();
        for (ConstantDef c : def.constants()) {
            if (overrides.containsKey(c.name())) {
                updatedConstants.add(new ConstantDef(c.name(), c.comment(),
                        overrides.get(c.name()), c.unit()));
            } else {
                updatedConstants.add(c);
            }
        }
        return new ModelDefinition(
                def.name(), def.comment(), def.moduleInterface(),
                def.stocks(), def.flows(), def.auxiliaries(), updatedConstants,
                def.lookupTables(), def.modules(), def.subscripts(), def.views(),
                def.defaultSimulation()
        );
    }

    private static ModelDefinition embedSettings(ModelDefinition def, SimulationSettings settings) {
        return new ModelDefinition(
                def.name(), def.comment(), def.moduleInterface(),
                def.stocks(), def.flows(), def.auxiliaries(), def.constants(),
                def.lookupTables(), def.modules(), def.subscripts(), def.views(),
                settings
        );
    }
}

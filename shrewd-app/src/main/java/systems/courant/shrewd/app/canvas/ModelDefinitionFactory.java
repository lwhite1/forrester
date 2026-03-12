package systems.courant.shrewd.app.canvas;

import systems.courant.shrewd.measure.Quantity;
import systems.courant.shrewd.measure.TimeUnit;
import systems.courant.shrewd.measure.UnitRegistry;
import systems.courant.shrewd.model.compile.CompiledModel;
import systems.courant.shrewd.model.compile.ModelCompiler;
import systems.courant.shrewd.model.def.VariableDef;
import systems.courant.shrewd.model.def.ModelDefinition;
import systems.courant.shrewd.model.def.ModelDefinitionBuilder;
import systems.courant.shrewd.model.def.SimulationSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Bridges {@link ModelDefinition} to the engine's analysis APIs by creating
 * model factory functions that compile fresh models with overridden parameter values.
 */
public final class ModelDefinitionFactory {

    /**
     * Shared registry for resolving built-in unit names. Safe to share because
     * the methods used here ({@link UnitRegistry#resolveTimeUnit}) are read-only
     * and never auto-create custom units.
     */
    private static final UnitRegistry SHARED_REGISTRY = new UnitRegistry();

    private ModelDefinitionFactory() {
    }

    /**
     * Creates a factory function that accepts a map of parameter name to value,
     * applies overrides to the definition, compiles, and returns the compiled model.
     * Returning {@link CompiledModel} (rather than bare {@link Model}) ensures that
     * sweep classes can create simulations with proper step synchronization,
     * which is required for time-dependent functions (STEP, RAMP, PULSE, etc.).
     */
    public static Function<Map<String, Double>, CompiledModel> createFactory(
            ModelDefinition def, SimulationSettings settings) {
        return paramMap -> {
            ModelDefinition overridden = applyParameterOverrides(def, paramMap);
            ModelDefinition withSettings = embedSettings(overridden, settings);
            ModelCompiler compiler = new ModelCompiler();
            return compiler.compile(withSettings);
        };
    }

    /**
     * Creates a single-parameter factory for use with {@link systems.courant.shrewd.sweep.ParameterSweep}.
     * Returns {@link CompiledModel} to ensure proper step synchronization.
     */
    public static java.util.function.DoubleFunction<CompiledModel> createSingleParamFactory(
            ModelDefinition def, SimulationSettings settings, String paramName) {
        return value -> {
            ModelDefinition overridden = applyParameterOverrides(def, Map.of(paramName, value));
            ModelDefinition withSettings = embedSettings(overridden, settings);
            ModelCompiler compiler = new ModelCompiler();
            return compiler.compile(withSettings);
        };
    }

    /**
     * Resolves the time step unit from simulation settings.
     */
    public static TimeUnit resolveTimeStep(SimulationSettings settings) {
        return SHARED_REGISTRY.resolveTimeUnit(settings.timeStep());
    }

    /**
     * Resolves the simulation duration as a {@link Quantity}.
     */
    public static Quantity resolveDuration(SimulationSettings settings) {
        TimeUnit durationUnit = SHARED_REGISTRY.resolveTimeUnit(settings.durationUnit());
        return new Quantity(settings.duration(), durationUnit);
    }

    /**
     * Creates a new ModelDefinition with modified parameter (literal-valued variable) values.
     * For each auxiliary whose name is in the override map, replaces its equation with the
     * formatted value string.
     */
    public static ModelDefinition applyParameterOverrides(
            ModelDefinition def, Map<String, Double> overrides) {
        if (overrides.isEmpty()) {
            return def;
        }
        List<VariableDef> updatedVariables = new ArrayList<>();
        for (VariableDef a : def.variables()) {
            if (overrides.containsKey(a.name())) {
                double value = overrides.get(a.name());
                updatedVariables.add(new VariableDef(a.name(), a.comment(),
                        formatValue(value), a.unit()));
            } else {
                updatedVariables.add(a);
            }
        }
        ModelDefinitionBuilder b = def.toBuilder();
        b.clearVariables();
        updatedVariables.forEach(b::variable);
        return b.build();
    }

    /**
     * Formats a double value as a string suitable for an variable equation.
     * Uses integer format when the value has no fractional part.
     */
    private static String formatValue(double value) {
        if (!Double.isInfinite(value) && !Double.isNaN(value)
                && Double.compare(value, Math.rint(value)) == 0
                && Math.abs(value) <= Long.MAX_VALUE) {
            return Long.toString((long) value);
        }
        return Double.toString(value);
    }

    private static ModelDefinition embedSettings(ModelDefinition def, SimulationSettings settings) {
        return def.toBuilder().defaultSimulation(settings).build();
    }
}

package com.deathrayresearch.forrester.model.def;

import java.util.List;

/**
 * Pure-data description of a complete system dynamics model. Contains no behavior or closures —
 * all formulas are stored as expression strings. This record can be serialized to/from JSON,
 * validated, and compiled into an executable {@code Model}.
 *
 * @param name the model name
 * @param comment optional description
 * @param moduleInterface optional module interface (non-null when this definition is used as a reusable module)
 * @param stocks the stock definitions
 * @param flows the flow definitions
 * @param auxiliaries the auxiliary variable definitions
 * @param constants the constant definitions
 * @param lookupTables the lookup table definitions
 * @param modules the module instance definitions (for nested/composite models)
 * @param subscripts the subscript definitions
 * @param cldVariables the causal loop diagram variable definitions
 * @param causalLinks the causal link definitions (CLD connections with polarity)
 * @param views the graphical view definitions
 * @param defaultSimulation optional default simulation settings
 */
public record ModelDefinition(
        String name,
        String comment,
        ModuleInterface moduleInterface,
        List<StockDef> stocks,
        List<FlowDef> flows,
        List<AuxDef> auxiliaries,
        List<ConstantDef> constants,
        List<LookupTableDef> lookupTables,
        List<ModuleInstanceDef> modules,
        List<SubscriptDef> subscripts,
        List<CldVariableDef> cldVariables,
        List<CausalLinkDef> causalLinks,
        List<ViewDef> views,
        SimulationSettings defaultSimulation
) {

    /**
     * Backward-compatible constructor without CLD fields.
     */
    public ModelDefinition(
            String name, String comment, ModuleInterface moduleInterface,
            List<StockDef> stocks, List<FlowDef> flows,
            List<AuxDef> auxiliaries, List<ConstantDef> constants,
            List<LookupTableDef> lookupTables, List<ModuleInstanceDef> modules,
            List<SubscriptDef> subscripts,
            List<ViewDef> views, SimulationSettings defaultSimulation) {
        this(name, comment, moduleInterface, stocks, flows, auxiliaries, constants,
                lookupTables, modules, subscripts, List.of(), List.of(),
                views, defaultSimulation);
    }

    public ModelDefinition {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Model name must not be blank");
        }
        stocks = stocks == null ? List.of() : List.copyOf(stocks);
        flows = flows == null ? List.of() : List.copyOf(flows);
        auxiliaries = auxiliaries == null ? List.of() : List.copyOf(auxiliaries);
        constants = constants == null ? List.of() : List.copyOf(constants);
        lookupTables = lookupTables == null ? List.of() : List.copyOf(lookupTables);
        modules = modules == null ? List.of() : List.copyOf(modules);
        subscripts = subscripts == null ? List.of() : List.copyOf(subscripts);
        cldVariables = cldVariables == null ? List.of() : List.copyOf(cldVariables);
        causalLinks = causalLinks == null ? List.of() : List.copyOf(causalLinks);
        views = views == null ? List.of() : List.copyOf(views);
    }
}

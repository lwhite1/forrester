package systems.courant.shrewd.model.def;

import systems.courant.shrewd.model.ModelMetadata;

import java.util.ArrayList;
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
 * @param variables the variable variable definitions (includes literal-valued parameters)
 * @param lookupTables the lookup table definitions
 * @param modules the module instance definitions (for nested/composite models)
 * @param subscripts the subscript definitions
 * @param cldVariables the causal loop diagram variable definitions
 * @param causalLinks the causal link definitions (CLD connections with polarity)
 * @param comments the free-text comment annotations (non-functional canvas decorations)
 * @param views the graphical view definitions
 * @param defaultSimulation optional default simulation settings
 * @param metadata optional attribution and licensing metadata
 * @param referenceDatasets observed/expected time-series datasets for model validation
 */
public record ModelDefinition(
        String name,
        String comment,
        ModuleInterface moduleInterface,
        List<StockDef> stocks,
        List<FlowDef> flows,
        List<VariableDef> variables,
        List<LookupTableDef> lookupTables,
        List<ModuleInstanceDef> modules,
        List<SubscriptDef> subscripts,
        List<CldVariableDef> cldVariables,
        List<CausalLinkDef> causalLinks,
        List<CommentDef> comments,
        List<ViewDef> views,
        SimulationSettings defaultSimulation,
        ModelMetadata metadata,
        List<ReferenceDataset> referenceDatasets
) {

    /**
     * Backward-compatible constructor without comments or reference datasets.
     */
    public ModelDefinition(
            String name, String comment, ModuleInterface moduleInterface,
            List<StockDef> stocks, List<FlowDef> flows,
            List<VariableDef> variables,
            List<LookupTableDef> lookupTables, List<ModuleInstanceDef> modules,
            List<SubscriptDef> subscripts,
            List<CldVariableDef> cldVariables, List<CausalLinkDef> causalLinks,
            List<ViewDef> views, SimulationSettings defaultSimulation,
            ModelMetadata metadata, List<ReferenceDataset> referenceDatasets) {
        this(name, comment, moduleInterface, stocks, flows, variables,
                lookupTables, modules, subscripts, cldVariables, causalLinks,
                List.of(), views, defaultSimulation, metadata, referenceDatasets);
    }

    /**
     * Backward-compatible constructor without comments, metadata, or reference datasets.
     */
    public ModelDefinition(
            String name, String comment, ModuleInterface moduleInterface,
            List<StockDef> stocks, List<FlowDef> flows,
            List<VariableDef> variables,
            List<LookupTableDef> lookupTables, List<ModuleInstanceDef> modules,
            List<SubscriptDef> subscripts,
            List<CldVariableDef> cldVariables, List<CausalLinkDef> causalLinks,
            List<ViewDef> views, SimulationSettings defaultSimulation,
            ModelMetadata metadata) {
        this(name, comment, moduleInterface, stocks, flows, variables,
                lookupTables, modules, subscripts, cldVariables, causalLinks,
                List.of(), views, defaultSimulation, metadata, List.of());
    }

    /**
     * Backward-compatible constructor without comments, metadata, or CLD fields.
     */
    public ModelDefinition(
            String name, String comment, ModuleInterface moduleInterface,
            List<StockDef> stocks, List<FlowDef> flows,
            List<VariableDef> variables,
            List<LookupTableDef> lookupTables, List<ModuleInstanceDef> modules,
            List<SubscriptDef> subscripts,
            List<CldVariableDef> cldVariables, List<CausalLinkDef> causalLinks,
            List<ViewDef> views, SimulationSettings defaultSimulation) {
        this(name, comment, moduleInterface, stocks, flows, variables,
                lookupTables, modules, subscripts, cldVariables, causalLinks,
                List.of(), views, defaultSimulation, null, List.of());
    }

    /**
     * Backward-compatible constructor without comments, CLD fields, or metadata.
     */
    public ModelDefinition(
            String name, String comment, ModuleInterface moduleInterface,
            List<StockDef> stocks, List<FlowDef> flows,
            List<VariableDef> variables,
            List<LookupTableDef> lookupTables, List<ModuleInstanceDef> modules,
            List<SubscriptDef> subscripts,
            List<ViewDef> views, SimulationSettings defaultSimulation) {
        this(name, comment, moduleInterface, stocks, flows, variables,
                lookupTables, modules, subscripts, List.of(), List.of(),
                List.of(), views, defaultSimulation, null, List.of());
    }

    public ModelDefinition {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Model name must not be blank");
        }
        stocks = stocks == null ? List.of() : List.copyOf(stocks);
        flows = flows == null ? List.of() : List.copyOf(flows);
        variables = variables == null ? List.of() : List.copyOf(variables);
        lookupTables = lookupTables == null ? List.of() : List.copyOf(lookupTables);
        modules = modules == null ? List.of() : List.copyOf(modules);
        subscripts = subscripts == null ? List.of() : List.copyOf(subscripts);
        cldVariables = cldVariables == null ? List.of() : List.copyOf(cldVariables);
        causalLinks = causalLinks == null ? List.of() : List.copyOf(causalLinks);
        comments = comments == null ? List.of() : List.copyOf(comments);
        views = views == null ? List.of() : List.copyOf(views);
        referenceDatasets = referenceDatasets == null ? List.of() : List.copyOf(referenceDatasets);
    }

    /**
     * Returns a builder pre-populated with this definition's values, allowing selective
     * modification without repeating all 14 constructor arguments.
     */
    public ModelDefinitionBuilder toBuilder() {
        ModelDefinitionBuilder b = new ModelDefinitionBuilder()
                .name(name)
                .comment(comment)
                .moduleInterface(moduleInterface)
                .defaultSimulation(defaultSimulation)
                .metadata(metadata);
        stocks.forEach(b::stock);
        flows.forEach(b::flow);
        variables.forEach(b::variable);
        lookupTables.forEach(b::lookupTable);
        modules.forEach(b::module);
        subscripts.forEach(s -> b.subscript(s.name(), s.labels()));
        cldVariables.forEach(b::cldVariable);
        causalLinks.forEach(b::causalLink);
        comments.forEach(b::comment);
        views.forEach(b::view);
        referenceDatasets.forEach(b::referenceDataset);
        return b;
    }

    /**
     * Returns the literal-valued variables (parameters) in this model.
     */
    public List<VariableDef> parameters() {
        return variables.stream().filter(VariableDef::isLiteral).toList();
    }

    /**
     * Returns the names of all literal-valued variables (parameters).
     */
    public List<String> parameterNames() {
        return variables.stream()
                .filter(VariableDef::isLiteral)
                .map(VariableDef::name)
                .toList();
    }

    /**
     * Migration helper: creates a ModelDefinition by merging a legacy constants list
     * into the variables list. Used by the JSON deserializer for backward compatibility
     * with files that have a separate "constants" array.
     */
    public static ModelDefinition withMigratedConstants(
            String name, String comment, ModuleInterface moduleInterface,
            List<StockDef> stocks, List<FlowDef> flows,
            List<VariableDef> variables, List<VariableDef> migratedConstants,
            List<LookupTableDef> lookupTables, List<ModuleInstanceDef> modules,
            List<SubscriptDef> subscripts,
            List<CldVariableDef> cldVariables, List<CausalLinkDef> causalLinks,
            List<CommentDef> comments,
            List<ViewDef> views, SimulationSettings defaultSimulation,
            ModelMetadata metadata) {
        List<VariableDef> merged = new ArrayList<>();
        if (variables != null) {
            merged.addAll(variables);
        }
        if (migratedConstants != null) {
            merged.addAll(migratedConstants);
        }
        return new ModelDefinition(name, comment, moduleInterface,
                stocks, flows, merged,
                lookupTables, modules, subscripts, cldVariables, causalLinks,
                comments, views, defaultSimulation, metadata, List.of());
    }
}

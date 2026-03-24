package systems.courant.sd.app.canvas;

import systems.courant.sd.model.ModelMetadata;
import systems.courant.sd.model.def.CausalLinkDef;
import systems.courant.sd.model.def.CldVariableDef;
import systems.courant.sd.model.def.CommentDef;
import systems.courant.sd.model.def.FlowDef;
import systems.courant.sd.model.def.LookupTableDef;
import systems.courant.sd.model.def.ModelDefinition;
import systems.courant.sd.model.def.ModuleInstanceDef;
import systems.courant.sd.model.def.ReferenceDataset;
import systems.courant.sd.model.def.SimulationSettings;
import systems.courant.sd.model.def.StockDef;
import systems.courant.sd.model.def.SubscriptDef;
import systems.courant.sd.model.def.VariableDef;
import systems.courant.sd.model.def.ViewDef;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * Read-only query facade over the model editor's mutable element lists.
 * Provides accessors, lookups, and immutable snapshot creation.
 * Extracted from {@link ModelEditor} to isolate the query surface.
 */
final class ModelQueryFacade {

    private final List<StockDef> stocks;
    private final List<FlowDef> flows;
    private final List<VariableDef> variables;
    private final List<ModuleInstanceDef> modules;
    private final List<LookupTableDef> lookupTables;
    private final List<CldVariableDef> cldVariables;
    private final List<CausalLinkDef> causalLinks;
    private final List<CommentDef> comments;
    private final List<SubscriptDef> subscripts;
    private final List<ReferenceDataset> referenceDatasets;
    private final Set<String> nameIndex;

    volatile String modelName = "Untitled";
    volatile String modelComment = "";
    volatile ModelMetadata metadata;
    volatile SimulationSettings simulationSettings;

    ModelQueryFacade(List<StockDef> stocks, List<FlowDef> flows,
                     List<VariableDef> variables, List<ModuleInstanceDef> modules,
                     List<LookupTableDef> lookupTables, List<CldVariableDef> cldVariables,
                     List<CausalLinkDef> causalLinks, List<CommentDef> comments,
                     List<SubscriptDef> subscripts, List<ReferenceDataset> referenceDatasets,
                     Set<String> nameIndex) {
        this.stocks = stocks;
        this.flows = flows;
        this.variables = variables;
        this.modules = modules;
        this.lookupTables = lookupTables;
        this.cldVariables = cldVariables;
        this.causalLinks = causalLinks;
        this.comments = comments;
        this.subscripts = subscripts;
        this.referenceDatasets = referenceDatasets;
        this.nameIndex = nameIndex;
    }

    // === Element collection accessors ===

    List<StockDef> getStocks() { return Collections.unmodifiableList(stocks); }

    List<FlowDef> getFlows() { return Collections.unmodifiableList(flows); }

    List<VariableDef> getVariables() { return Collections.unmodifiableList(variables); }

    List<ModuleInstanceDef> getModules() { return Collections.unmodifiableList(modules); }

    List<LookupTableDef> getLookupTables() { return Collections.unmodifiableList(lookupTables); }

    List<CldVariableDef> getCldVariables() { return Collections.unmodifiableList(cldVariables); }

    List<CausalLinkDef> getCausalLinks() { return Collections.unmodifiableList(causalLinks); }

    List<CommentDef> getComments() { return Collections.unmodifiableList(comments); }

    List<SubscriptDef> getSubscripts() { return Collections.unmodifiableList(subscripts); }

    List<ReferenceDataset> getReferenceDatasets() { return Collections.unmodifiableList(referenceDatasets); }

    // === Single-element lookups ===

    Optional<StockDef> getStockByName(String name) {
        return findByName(stocks, name, StockDef::name);
    }

    Optional<FlowDef> getFlowByName(String name) {
        return findByName(flows, name, FlowDef::name);
    }

    Optional<VariableDef> getVariableByName(String name) {
        return findByName(variables, name, VariableDef::name);
    }

    Optional<ModuleInstanceDef> getModuleByName(String name) {
        return findByName(modules, name, ModuleInstanceDef::instanceName);
    }

    Optional<LookupTableDef> getLookupTableByName(String name) {
        return findByName(lookupTables, name, LookupTableDef::name);
    }

    Optional<CldVariableDef> getCldVariableByName(String name) {
        return findByName(cldVariables, name, CldVariableDef::name);
    }

    CommentDef getCommentByName(String name) {
        for (CommentDef c : comments) {
            if (c.name().equals(name)) {
                return c;
            }
        }
        return null;
    }

    /**
     * Returns the subscript dimension names for the given element, or an empty list
     * if the element is scalar or not found.
     */
    List<String> getElementSubscripts(String name) {
        Optional<StockDef> stock = getStockByName(name);
        if (stock.isPresent()) {
            return stock.get().subscripts();
        }
        Optional<FlowDef> flow = getFlowByName(name);
        if (flow.isPresent()) {
            return flow.get().subscripts();
        }
        Optional<VariableDef> variable = getVariableByName(name);
        if (variable.isPresent()) {
            return variable.get().subscripts();
        }
        return List.of();
    }

    // === Derived queries ===

    boolean hasElement(String name) { return nameIndex.contains(name); }

    Optional<String> getStockUnit(String name) {
        return getStockByName(name).map(StockDef::unit);
    }

    Optional<String> getFlowEquation(String name) {
        return getFlowByName(name).map(FlowDef::equation);
    }

    Optional<String> getVariableEquation(String name) {
        return getVariableByName(name).map(VariableDef::equation);
    }

    List<String> getParameterNames() {
        return variables.stream()
                .filter(VariableDef::isLiteral)
                .map(VariableDef::name)
                .toList();
    }

    int getModuleIndex(String name) {
        for (int i = 0; i < modules.size(); i++) {
            if (modules.get(i).instanceName().equals(name)) {
                return i;
            }
        }
        return -1;
    }

    // === Model metadata ===

    String getModelName() { return modelName; }

    String getModelComment() { return modelComment; }

    ModelMetadata getMetadata() { return metadata; }

    SimulationSettings getSimulationSettings() { return simulationSettings; }

    // === Snapshot creation ===

    ModelDefinition toModelDefinition() {
        return toModelDefinition(null);
    }

    ModelDefinition toModelDefinition(ViewDef view) {
        return new ModelDefinition(
                modelName,
                modelComment.isBlank() ? null : modelComment,
                null,
                List.copyOf(stocks),
                List.copyOf(flows),
                List.copyOf(variables),
                List.copyOf(lookupTables),
                List.copyOf(modules),
                List.copyOf(subscripts),
                List.copyOf(cldVariables),
                List.copyOf(causalLinks),
                List.copyOf(comments),
                view != null ? List.of(view) : List.of(),
                simulationSettings,
                metadata,
                List.copyOf(referenceDatasets)
        );
    }

    // === Internal helper ===

    <T> Optional<T> findByName(List<T> list, String name, Function<T, String> nameGetter) {
        for (T item : list) {
            if (nameGetter.apply(item).equals(name)) {
                return Optional.of(item);
            }
        }
        return Optional.empty();
    }
}

package systems.courant.sd.app.canvas;

import systems.courant.sd.model.ModelMetadata;
import systems.courant.sd.model.def.VariableDef;
import systems.courant.sd.model.def.CausalLinkDef;
import systems.courant.sd.model.def.CldVariableDef;
import systems.courant.sd.model.def.CommentDef;
import systems.courant.sd.model.def.ConnectorRoute;
import systems.courant.sd.model.def.ElementType;
import systems.courant.sd.model.def.FlowDef;
import systems.courant.sd.model.def.LookupTableDef;
import systems.courant.sd.model.def.ModelDefinition;
import systems.courant.sd.model.def.ModuleInstanceDef;
import systems.courant.sd.model.def.ModuleInterface;
import systems.courant.sd.model.def.ReferenceDataset;
import systems.courant.sd.model.def.SimulationSettings;
import systems.courant.sd.model.def.StockDef;
import systems.courant.sd.model.def.SubscriptDef;
import systems.courant.sd.model.def.ViewDef;

import javafx.application.Platform;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * Mutable model editing layer that sits between the UI and the engine's immutable
 * {@link ModelDefinition}. Supports adding and removing elements while preserving
 * the engine's immutability contract — an immutable snapshot can be rebuilt on demand
 * via {@link #toModelDefinition()}.
 *
 * <p>Thread confinement: all mutable state must be accessed on the JavaFX Application
 * Thread. The only safe cross-thread operation is {@link #toModelDefinition()}, which
 * builds an immutable snapshot for background analysis tasks. Element lists use
 * {@link CopyOnWriteArrayList} to allow safe snapshot iteration from background threads.</p>
 */
public class ModelEditor {

    private static final Logger log = LoggerFactory.getLogger(ModelEditor.class);

    private final List<StockDef> stocks = new CopyOnWriteArrayList<>();
    private final List<FlowDef> flows = new CopyOnWriteArrayList<>();
    private final List<VariableDef> variables = new CopyOnWriteArrayList<>();
    private final List<ModuleInstanceDef> modules = new CopyOnWriteArrayList<>();
    private final List<LookupTableDef> lookupTables = new CopyOnWriteArrayList<>();
    private final List<CldVariableDef> cldVariables = new CopyOnWriteArrayList<>();
    private final List<CausalLinkDef> causalLinks = new CopyOnWriteArrayList<>();
    private final List<CommentDef> comments = new CopyOnWriteArrayList<>();
    private final List<SubscriptDef> subscripts = new CopyOnWriteArrayList<>();
    private final List<ReferenceDataset> referenceDatasets = new CopyOnWriteArrayList<>();
    private final Set<String> nameIndex = ConcurrentHashMap.newKeySet();
    private final List<ModelEditListener> listeners = new CopyOnWriteArrayList<>();

    private final EquationReferenceManager equationRefManager =
            new EquationReferenceManager(flows, variables);
    private final ElementFactory factory = new ElementFactory(
            stocks, flows, variables, modules, lookupTables, cldVariables,
            causalLinks, comments, nameIndex, this::resolveDefaultTimeUnit);
    private final ElementCascadeManager cascadeManager = new ElementCascadeManager(
            stocks, flows, variables, modules, lookupTables, cldVariables,
            causalLinks, comments, nameIndex, equationRefManager);
    private final FlowConnectionManager flowConnMgr = new FlowConnectionManager(
            flows, modules, nameIndex, equationRefManager);
    private final ModelQueryFacade queryFacade = new ModelQueryFacade(
            stocks, flows, variables, modules, lookupTables, cldVariables,
            causalLinks, comments, subscripts, referenceDatasets, nameIndex);

    // ── Listeners ────────────────────────────────────────────────────────

    public void addListener(ModelEditListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeListener(ModelEditListener listener) {
        listeners.remove(listener);
    }

    private static final boolean HEADLESS_TEST =
            Boolean.getBoolean("testfx.headless");

    private static void checkFxThread() {
        if (!HEADLESS_TEST && !Platform.isFxApplicationThread()) {
            throw new IllegalStateException(
                    "ModelEditor must be accessed on the JavaFX Application Thread");
        }
    }

    private void fireElementAdded(String name, String typeName) {
        for (ModelEditListener l : listeners) { l.onElementAdded(name, typeName); }
    }

    private void fireElementRemoved(String name) {
        for (ModelEditListener l : listeners) { l.onElementRemoved(name); }
    }

    private void fireElementRenamed(String oldName, String newName) {
        for (ModelEditListener l : listeners) { l.onElementRenamed(oldName, newName); }
    }

    private void fireEquationChanged(String elementName) {
        for (ModelEditListener l : listeners) { l.onEquationChanged(elementName); }
    }

    private void fireModelMetadataChanged() {
        for (ModelEditListener l : listeners) { l.onModelMetadataChanged(); }
    }

    // ── Load / Snapshot ──────────────────────────────────────────────────

    /**
     * Loads all elements from an immutable {@link ModelDefinition} into mutable lists,
     * clearing any previous state.
     */
    public void loadFrom(ModelDefinition definition) {
        checkFxThread();
        queryFacade.modelName = definition.name();
        queryFacade.modelComment = definition.comment() != null ? definition.comment() : "";
        replaceContents(stocks, definition.stocks());
        replaceContents(flows, definition.flows());
        replaceContents(variables, definition.variables());
        replaceContents(modules, definition.modules());
        replaceContents(lookupTables, definition.lookupTables());
        replaceContents(cldVariables, definition.cldVariables());
        replaceContents(causalLinks, definition.causalLinks());
        replaceContents(comments, definition.comments());
        replaceContents(subscripts, definition.subscripts());
        replaceContents(referenceDatasets, definition.referenceDatasets());
        nameIndex.clear();
        queryFacade.simulationSettings = definition.defaultSimulation();
        queryFacade.metadata = definition.metadata();

        stocks.forEach(s -> nameIndex.add(s.name()));
        flows.forEach(f -> nameIndex.add(f.name()));
        variables.forEach(a -> nameIndex.add(a.name()));
        modules.forEach(m -> nameIndex.add(m.instanceName()));
        lookupTables.forEach(lt -> nameIndex.add(lt.name()));
        cldVariables.forEach(v -> nameIndex.add(v.name()));
        comments.forEach(c -> nameIndex.add(c.name()));

        factory.resetCounters();
    }

    // ── Element creation ─────────────────────────────────────────────────

    /**
     * Replaces the contents of a list without creating an intermediate empty state
     * visible to snapshot iterators on background threads. New elements are appended
     * first, then old elements are removed, so concurrent readers see either the old
     * contents, a union of old and new, or the new contents — never an empty list
     * (unless the new contents are themselves empty).
     */
    private static <T> void replaceContents(List<T> list, List<? extends T> newContents) {
        int oldSize = list.size();
        if (oldSize == 0) {
            if (!newContents.isEmpty()) {
                list.addAll(newContents);
            }
        } else if (newContents.isEmpty()) {
            list.clear();
        } else {
            list.addAll(newContents);
            list.subList(0, oldSize).clear();
        }
    }

    /** @return the name of the created stock */
    public String addStock() {
        checkFxThread();
        String name = factory.addStock();
        fireElementAdded(name, "Stock");
        return name;
    }

    public String addFlow() {
        checkFxThread();
        return addFlow(null, null);
    }

    /** @return the name of the created flow */
    public String addFlow(String source, String sink) {
        checkFxThread();
        String name = factory.addFlow(source, sink);
        fireElementAdded(name, "Flow");
        return name;
    }

    /** @return the name of the created auxiliary */
    public String addVariable() {
        checkFxThread();
        String name = factory.addVariable();
        fireElementAdded(name, "Variable");
        return name;
    }

    /** @return the name of the created stock */
    public String addStockFrom(StockDef template) {
        checkFxThread();
        String name = factory.addStockFrom(template);
        fireElementAdded(name, "Stock");
        return name;
    }

    /** @return the name of the created flow */
    public String addFlowFrom(FlowDef template, String source, String sink) {
        checkFxThread();
        String name = factory.addFlowFrom(template, source, sink);
        fireElementAdded(name, "Flow");
        return name;
    }

    /** @return the name of the created auxiliary */
    public String addVariableFrom(VariableDef template, String equation) {
        checkFxThread();
        String name = factory.addVariableFrom(template, equation);
        fireElementAdded(name, "Variable");
        return name;
    }

    /** @return the instance name of the created module */
    public String addModuleFrom(ModuleInstanceDef template) {
        checkFxThread();
        String name = factory.addModuleFrom(template);
        fireElementAdded(name, "Module");
        return name;
    }

    /** @return the instance name of the created module */
    public String addModule() {
        checkFxThread();
        String name = factory.addModule();
        fireElementAdded(name, "Module");
        return name;
    }

    /** @return the name of the created lookup table */
    public String addLookup() {
        checkFxThread();
        String name = factory.addLookup();
        fireElementAdded(name, "Lookup");
        return name;
    }

    /** @return the name of the created lookup table */
    public String addLookupFrom(LookupTableDef template) {
        checkFxThread();
        String name = factory.addLookupFrom(template);
        fireElementAdded(name, "Lookup");
        return name;
    }

    /** @return the name of the created CLD variable */
    public String addCldVariable() {
        checkFxThread();
        String name = factory.addCldVariable();
        fireElementAdded(name, "CLD Variable");
        return name;
    }

    /** @return the name of the created CLD variable */
    public String addCldVariableFrom(CldVariableDef template) {
        checkFxThread();
        String name = factory.addCldVariableFrom(template);
        fireElementAdded(name, "CLD Variable");
        return name;
    }

    /** @return the name of the created comment */
    public String addComment() {
        checkFxThread();
        String name = factory.addComment();
        fireElementAdded(name, "Comment");
        return name;
    }

    /** @return the name of the created comment */
    public String addCommentFrom(CommentDef template) {
        checkFxThread();
        String name = factory.addCommentFrom(template);
        fireElementAdded(name, "Comment");
        return name;
    }

    /**
     * Adds a causal link between two elements.
     * @return true if both endpoints exist and the link was added
     */
    public boolean addCausalLink(String from, String to, CausalLinkDef.Polarity polarity) {
        checkFxThread();
        return factory.addCausalLink(from, to, polarity);
    }

    /** Adds a reference dataset for model validation overlay. */
    public void addReferenceDataset(ReferenceDataset dataset) {
        checkFxThread();
        referenceDatasets.add(dataset);
    }

    // ── Remove / Rename ──────────────────────────────────────────────────

    /**
     * Removes the element with the given name from the appropriate list.
     * If a stock is removed, any flow referencing it as source or sink has
     * that connection nullified (becomes a cloud).
     */
    public void removeElement(String name) {
        checkFxThread();
        List<String> affectedElements = cascadeManager.remove(name);
        for (String affected : affectedElements) {
            fireEquationChanged(affected);
        }
        fireElementRemoved(name);
    }

    /**
     * Renames an element across all model data. Updates the element's own name in
     * the appropriate list, updates flow source/sink references, and updates equation
     * references using the underscore token convention.
     *
     * @return true if the element was found and renamed
     */
    public boolean renameElement(String oldName, String newName) {
        checkFxThread();
        boolean renamed = cascadeManager.rename(oldName, newName);
        if (renamed) {
            fireElementRenamed(oldName, newName);
        }
        return renamed;
    }

    /**
     * Removes a causal link between two elements.
     * @return true if the link was found and removed
     */
    public boolean removeCausalLink(String from, String to) {
        checkFxThread();
        return causalLinks.removeIf(link -> link.from().equals(from) && link.to().equals(to));
    }

    // ── Flow connections and info-link rerouting ─────────────────────────

    /**
     * Reconnects a flow endpoint to a different stock, or disconnects it (pass null for stockName).
     */
    public boolean reconnectFlow(String flowName, FlowEndpointCalculator.FlowEnd end,
                                 String stockName) {
        checkFxThread();
        return flowConnMgr.reconnectFlow(flowName, end, stockName);
    }

    /** @return true if a reference was found and removed */
    public boolean removeConnectionReference(String fromName, String toName) {
        checkFxThread();
        return flowConnMgr.removeConnectionReference(fromName, toName);
    }

    /** @return true if the equation was updated */
    public boolean rerouteConnectionSource(String oldFrom, String newFrom, String to) {
        checkFxThread();
        return flowConnMgr.rerouteConnectionSource(oldFrom, newFrom, to);
    }

    /** @return true if the reroute was performed */
    public boolean rerouteConnectionTarget(String from, String oldTo, String newTo) {
        checkFxThread();
        return flowConnMgr.rerouteConnectionTarget(from, oldTo, newTo);
    }

    /**
     * Generates connector routes from the current model state's dependency graph,
     * including binding-derived connectors for module input/output bindings.
     */
    public List<ConnectorRoute> generateConnectors() {
        return flowConnMgr.generateConnectors(toModelDefinition());
    }

    // ── Property setters ─────────────────────────────────────────────────

    /** @return true if the lookup table was found and updated */
    public boolean setLookupTable(String name, LookupTableDef updated) {
        checkFxThread();
        for (int i = 0; i < lookupTables.size(); i++) {
            if (lookupTables.get(i).name().equals(name)) {
                lookupTables.set(i, updated);
                return true;
            }
        }
        return false;
    }

    /** @return true if the flow was found and updated */
    public boolean setFlowEquation(String name, String equation) {
        checkFxThread();
        if (equation == null || equation.isBlank()) {
            return false;
        }
        boolean updated = updateInList(flows, name, FlowDef::name,
                f -> new FlowDef(f.name(), f.comment(), equation,
                        f.timeUnit(), f.materialUnit(), f.source(), f.sink(), f.subscripts()));
        if (updated) {
            fireEquationChanged(name);
        }
        return updated;
    }

    /** @return true if the variable was found and updated */
    public boolean setVariableEquation(String name, String equation) {
        checkFxThread();
        if (equation == null || equation.isBlank()) {
            return false;
        }
        boolean updated = updateInList(variables, name, VariableDef::name,
                a -> new VariableDef(a.name(), a.comment(), equation, a.unit(), a.subscripts()));
        if (updated) {
            fireEquationChanged(name);
        }
        return updated;
    }

    /** @return true if the stock was found and updated */
    public boolean setStockInitialValue(String name, double value) {
        checkFxThread();
        return updateInList(stocks, name, StockDef::name,
                s -> new StockDef(name, s.comment(), value,
                        s.unit(), s.negativeValuePolicy(), s.subscripts()));
    }

    /** @return true if the stock was found and updated */
    public boolean setStockUnit(String name, String unit) {
        checkFxThread();
        if (unit == null) {
            return false;
        }
        return updateInList(stocks, name, StockDef::name,
                s -> new StockDef(name, s.comment(), s.initialValue(),
                        unit, s.negativeValuePolicy(), s.subscripts()));
    }

    /** @return true if the stock was found and updated */
    public boolean setStockNegativeValuePolicy(String name, String policy) {
        checkFxThread();
        return updateInList(stocks, name, StockDef::name,
                s -> new StockDef(name, s.comment(), s.initialValue(),
                        s.unit(), policy, s.subscripts()));
    }

    /** @return true if the flow was found and updated */
    public boolean setFlowTimeUnit(String name, String timeUnit) {
        checkFxThread();
        if (timeUnit == null || timeUnit.isBlank()) {
            return false;
        }
        return updateInList(flows, name, FlowDef::name,
                f -> new FlowDef(f.name(), f.comment(), f.equation(),
                        timeUnit, f.materialUnit(), f.source(), f.sink(), f.subscripts()));
    }

    /** @return true if the flow was found and updated */
    public boolean setFlowMaterialUnit(String name, String materialUnit) {
        checkFxThread();
        return updateInList(flows, name, FlowDef::name,
                f -> new FlowDef(f.name(), f.comment(), f.equation(),
                        f.timeUnit(), materialUnit, f.source(), f.sink(), f.subscripts()));
    }

    /** @return true if the variable was found and updated */
    public boolean setVariableUnit(String name, String unit) {
        checkFxThread();
        if (unit == null) {
            return false;
        }
        return updateInList(variables, name, VariableDef::name,
                a -> new VariableDef(a.name(), a.comment(), a.equation(), unit, a.subscripts()));
    }

    /** @return true if the stock was found and updated */
    public boolean setStockComment(String name, String comment) {
        checkFxThread();
        return updateInList(stocks, name, StockDef::name,
                s -> new StockDef(name, comment, s.initialValue(),
                        s.unit(), s.negativeValuePolicy(), s.subscripts()));
    }

    /** @return true if the flow was found and updated */
    public boolean setFlowComment(String name, String comment) {
        checkFxThread();
        return updateInList(flows, name, FlowDef::name,
                f -> new FlowDef(f.name(), comment, f.equation(),
                        f.timeUnit(), f.materialUnit(), f.source(), f.sink(), f.subscripts()));
    }

    /** @return true if the variable was found and updated */
    public boolean setVariableComment(String name, String comment) {
        checkFxThread();
        return updateInList(variables, name, VariableDef::name,
                a -> new VariableDef(a.name(), comment, a.equation(), a.unit(), a.subscripts()));
    }

    // ── Subscript assignment ─────────────────────────────────────────────

    /** @return true if the stock was found and updated */
    public boolean setStockSubscripts(String name, List<String> newSubscripts) {
        checkFxThread();
        return updateInList(stocks, name, StockDef::name,
                s -> new StockDef(name, s.comment(), s.initialValue(),
                        s.unit(), s.negativeValuePolicy(), newSubscripts));
    }

    /** @return true if the flow was found and updated */
    public boolean setFlowSubscripts(String name, List<String> newSubscripts) {
        checkFxThread();
        return updateInList(flows, name, FlowDef::name,
                f -> new FlowDef(f.name(), f.comment(), f.equation(),
                        f.timeUnit(), f.materialUnit(), f.source(), f.sink(), newSubscripts));
    }

    /** @return true if the variable was found and updated */
    public boolean setVariableSubscripts(String name, List<String> newSubscripts) {
        checkFxThread();
        return updateInList(variables, name, VariableDef::name,
                a -> new VariableDef(a.name(), a.comment(), a.equation(), a.unit(), newSubscripts));
    }

    // ── Subscript definition management ─────────────────────────────────

    public void addSubscript(SubscriptDef def) {
        checkFxThread();
        subscripts.add(def);
    }

    public boolean updateSubscript(String oldName, SubscriptDef updated) {
        checkFxThread();
        for (int i = 0; i < subscripts.size(); i++) {
            if (subscripts.get(i).name().equals(oldName)) {
                subscripts.set(i, updated);
                return true;
            }
        }
        return false;
    }

    public boolean removeSubscript(String name) {
        checkFxThread();
        return subscripts.removeIf(s -> s.name().equals(name));
    }

    /** @return true if the lookup table was found and updated */
    public boolean setLookupComment(String name, String comment) {
        checkFxThread();
        return updateInList(lookupTables, name, LookupTableDef::name,
                lt -> new LookupTableDef(name, comment,
                        lt.xValues(), lt.yValues(), lt.interpolation(), lt.unit()));
    }

    /** @return true if the lookup table was found and updated */
    public boolean setLookupUnit(String name, String unit) {
        checkFxThread();
        return updateInList(lookupTables, name, LookupTableDef::name,
                lt -> new LookupTableDef(lt.name(), lt.comment(),
                        lt.xValues(), lt.yValues(), lt.interpolation(), unit));
    }

    /** @return true if the CLD variable was found and updated */
    public boolean setCldVariableComment(String name, String comment) {
        checkFxThread();
        return updateInList(cldVariables, name, CldVariableDef::name,
                v -> new CldVariableDef(name, comment));
    }

    /** @return true if the causal link was found and updated */
    public boolean setCausalLinkPolarity(String from, String to, CausalLinkDef.Polarity polarity) {
        checkFxThread();
        for (int i = 0; i < causalLinks.size(); i++) {
            CausalLinkDef link = causalLinks.get(i);
            if (link.from().equals(from) && link.to().equals(to)) {
                causalLinks.set(i, new CausalLinkDef(from, to, polarity, link.comment(), link.strength()));
                return true;
            }
        }
        return false;
    }

    /** @return true if the causal link was found and its strength updated */
    public boolean setCausalLinkStrength(String from, String to, double strength) {
        checkFxThread();
        for (int i = 0; i < causalLinks.size(); i++) {
            CausalLinkDef link = causalLinks.get(i);
            if (link.from().equals(from) && link.to().equals(to)) {
                causalLinks.set(i, link.withStrength(strength));
                return true;
            }
        }
        return false;
    }

    /** @return true if the comment was found and updated */
    public boolean setCommentText(String name, String text) {
        checkFxThread();
        return updateInList(comments, name, CommentDef::name,
                c -> new CommentDef(name, text));
    }

    public void setSimulationSettings(SimulationSettings simulationSettings) {
        checkFxThread();
        queryFacade.simulationSettings = simulationSettings;
    }

    public void setModelName(String name) {
        checkFxThread();
        if (name != null && !name.isBlank() && !name.equals(queryFacade.modelName)) {
            queryFacade.modelName = name;
            fireModelMetadataChanged();
        }
    }

    public void setModelComment(String comment) {
        checkFxThread();
        String value = comment != null ? comment : "";
        if (!value.equals(queryFacade.modelComment)) {
            queryFacade.modelComment = value;
            fireModelMetadataChanged();
        }
    }

    public void setMetadata(ModelMetadata metadata) {
        checkFxThread();
        queryFacade.metadata = metadata;
    }

    private <T> boolean updateInList(List<T> list, String name,
                                      Function<T, String> nameGetter,
                                      UnaryOperator<T> updater) {
        return ElementCascadeManager.updateInList(list, name, nameGetter, updater);
    }

    // ── Module mutation ──────────────────────────────────────────────────

    /**
     * Replaces the definition of the module at the given index, preserving
     * the instance name and bindings.
     */
    public void updateModuleDefinition(int index, ModelDefinition newDef) {
        checkFxThread();
        if (index < 0 || index >= modules.size()) {
            return;
        }
        ModuleInstanceDef existing = modules.get(index);
        modules.set(index, new ModuleInstanceDef(
                existing.instanceName(), newDef,
                existing.inputBindings(), existing.outputBindings()));
    }

    /**
     * Updates the module interface (port definitions) of the module with the given name.
     *
     * @return true if the module was found and updated
     */
    public boolean updateModuleInterface(String name, ModuleInterface newInterface) {
        checkFxThread();
        for (int i = 0; i < modules.size(); i++) {
            if (modules.get(i).instanceName().equals(name)) {
                ModuleInstanceDef m = modules.get(i);
                ModelDefinition oldDef = m.definition();
                ModelDefinition newDef = oldDef.toBuilder()
                        .moduleInterface(newInterface)
                        .build();
                modules.set(i, new ModuleInstanceDef(
                        m.instanceName(), newDef,
                        m.inputBindings(), m.outputBindings()));
                return true;
            }
        }
        return false;
    }

    /**
     * Updates the input and output bindings of the module with the given name.
     *
     * @return true if the module was found and updated
     */
    public boolean updateModuleBindings(String name,
                                         Map<String, String> inputBindings,
                                         Map<String, String> outputBindings) {
        checkFxThread();
        for (int i = 0; i < modules.size(); i++) {
            if (modules.get(i).instanceName().equals(name)) {
                ModuleInstanceDef m = modules.get(i);
                modules.set(i, new ModuleInstanceDef(
                        m.instanceName(), m.definition(),
                        inputBindings, outputBindings));
                return true;
            }
        }
        return false;
    }

    // ── CLD classification ───────────────────────────────────────────────

    /**
     * Classifies a CLD variable by converting it into a stock-and-flow element type.
     * The CLD variable is removed and replaced with the target element type at the
     * same name. Causal links involving the classified variable are removed (info links
     * will auto-generate from equation dependencies once equations are added).
     *
     * @param name       the CLD variable name
     * @param targetType the target element type (STOCK, FLOW, AUX, or CONSTANT)
     * @return true if the variable was found and classified
     */
    public boolean classifyCldVariable(String name, ElementType targetType) {
        checkFxThread();

        Optional<CldVariableDef> opt = getCldVariableByName(name);
        if (opt.isEmpty()) {
            return false;
        }
        CldVariableDef variable = opt.get();

        // Remove the CLD variable
        cldVariables.removeIf(v -> v.name().equals(name));

        // Create the target S&F element with the same name
        switch (targetType) {
            case STOCK -> stocks.add(new StockDef(name, variable.comment(), 0, "units", null));
            case FLOW -> flows.add(new FlowDef(name, variable.comment(), "0",
                    resolveDefaultTimeUnit(), null, null));
            case AUX -> variables.add(new VariableDef(name, variable.comment(), "0", "units"));
            default -> {
                // Unsupported target type — put the variable back
                cldVariables.add(variable);
                return false;
            }
        }

        // Remove causal links that reference this variable
        causalLinks.removeIf(link -> link.from().equals(name) || link.to().equals(name));

        fireElementRemoved(name);
        fireElementAdded(name, targetType.name());
        return true;
    }

    /**
     * Converts a variable to a comment element. The comment text is set to
     * "name: equation" to preserve the variable's information.
     *
     * @return true if the variable was found and converted
     */
    public boolean convertVariableToComment(String name) {
        checkFxThread();

        Optional<VariableDef> opt = getVariableByName(name);
        if (opt.isEmpty()) {
            return false;
        }
        VariableDef variable = opt.get();

        String text = name + ": " + (variable.equation() != null ? variable.equation() : "");
        if (variable.comment() != null && !variable.comment().isBlank()) {
            text += "\n" + variable.comment();
        }

        variables.removeIf(v -> v.name().equals(name));
        comments.add(new CommentDef(name, text));

        fireElementRemoved(name);
        fireElementAdded(name, ElementType.COMMENT.name());
        return true;
    }

    // ── Query delegations (read-only) ────────────────────────────────────

    public boolean hasElement(String name) { return queryFacade.hasElement(name); }
    public String getModelName() { return queryFacade.getModelName(); }
    public String getModelComment() { return queryFacade.getModelComment(); }
    public ModelMetadata getMetadata() { return queryFacade.getMetadata(); }
    public SimulationSettings getSimulationSettings() { return queryFacade.getSimulationSettings(); }

    private String resolveDefaultTimeUnit() {
        SimulationSettings settings = getSimulationSettings();
        return settings != null ? settings.timeStep() : "Day";
    }
    public List<StockDef> getStocks() { return queryFacade.getStocks(); }
    public List<FlowDef> getFlows() { return queryFacade.getFlows(); }
    public List<VariableDef> getVariables() { return queryFacade.getVariables(); }
    public List<SubscriptDef> getSubscripts() { return queryFacade.getSubscripts(); }
    public List<ModuleInstanceDef> getModules() { return queryFacade.getModules(); }
    public List<LookupTableDef> getLookupTables() { return queryFacade.getLookupTables(); }
    public List<CldVariableDef> getCldVariables() { return queryFacade.getCldVariables(); }
    public List<CausalLinkDef> getCausalLinks() { return queryFacade.getCausalLinks(); }
    public List<CommentDef> getComments() { return queryFacade.getComments(); }
    public List<ReferenceDataset> getReferenceDatasets() { return queryFacade.getReferenceDatasets(); }
    public List<String> getParameterNames() { return queryFacade.getParameterNames(); }
    public Optional<StockDef> getStockByName(String name) { return queryFacade.getStockByName(name); }
    public Optional<FlowDef> getFlowByName(String name) { return queryFacade.getFlowByName(name); }
    public Optional<VariableDef> getVariableByName(String name) { return queryFacade.getVariableByName(name); }
    public Optional<ModuleInstanceDef> getModuleByName(String name) { return queryFacade.getModuleByName(name); }
    public Optional<LookupTableDef> getLookupTableByName(String name) { return queryFacade.getLookupTableByName(name); }
    public Optional<CldVariableDef> getCldVariableByName(String name) { return queryFacade.getCldVariableByName(name); }
    public CommentDef getCommentByName(String name) { return queryFacade.getCommentByName(name); }
    public Optional<String> getStockUnit(String name) { return queryFacade.getStockUnit(name); }
    public Optional<String> getFlowEquation(String name) { return queryFacade.getFlowEquation(name); }
    public Optional<String> getVariableEquation(String name) { return queryFacade.getVariableEquation(name); }
    public int getModuleIndex(String name) { return queryFacade.getModuleIndex(name); }
    public List<String> getElementSubscripts(String name) { return queryFacade.getElementSubscripts(name); }
    public ModelDefinition toModelDefinition() { return toModelDefinition(null); }
    public ModelDefinition toModelDefinition(ViewDef view) { return queryFacade.toModelDefinition(view); }

    public static final int MAX_NAME_LENGTH = ElementNameValidator.MAX_NAME_LENGTH;

    public static boolean isValidName(String name) {
        return ElementNameValidator.isValidName(name);
    }
}

package systems.courant.shrewd.app.canvas;

import systems.courant.shrewd.model.ModelMetadata;
import systems.courant.shrewd.model.def.AuxDef;
import systems.courant.shrewd.model.def.CausalLinkDef;
import systems.courant.shrewd.model.def.CldVariableDef;
import systems.courant.shrewd.model.def.ConnectorRoute;
import systems.courant.shrewd.model.def.ElementType;
import systems.courant.shrewd.model.def.FlowDef;
import systems.courant.shrewd.model.def.LookupTableDef;
import systems.courant.shrewd.model.def.ModelDefinition;
import systems.courant.shrewd.model.def.ModuleInstanceDef;
import systems.courant.shrewd.model.def.ModuleInterface;
import systems.courant.shrewd.model.def.ReferenceDataset;
import systems.courant.shrewd.model.def.SimulationSettings;
import systems.courant.shrewd.model.def.StockDef;
import systems.courant.shrewd.model.def.ViewDef;
import systems.courant.shrewd.model.graph.ConnectorGenerator;

import javafx.application.Platform;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import static systems.courant.shrewd.app.canvas.ElementNameValidator.parseIdSuffix;
import static systems.courant.shrewd.app.canvas.ElementNameValidator.resolveUniqueName;
import static systems.courant.shrewd.app.canvas.EquationReferenceManager.replaceToken;

/**
 * Mutable model editing layer that sits between the UI and the engine's immutable
 * {@link ModelDefinition}. Supports adding and removing elements while preserving
 * the engine's immutability contract — an immutable snapshot can be rebuilt on demand
 * via {@link #toModelDefinition()}.
 *
 * <p>Thread confinement: all mutable state must be accessed on the JavaFX Application
 * Thread. The only safe cross-thread operation is {@link #toModelDefinition()}, which
 * builds an immutable snapshot for background analysis tasks.</p>
 */
public class ModelEditor {

    private static final Logger log = LoggerFactory.getLogger(ModelEditor.class);

    private String modelName = "Untitled";
    private String modelComment = "";
    private ModelMetadata metadata;
    private final List<StockDef> stocks = new ArrayList<>();
    private final List<FlowDef> flows = new ArrayList<>();
    private final List<AuxDef> auxiliaries = new ArrayList<>();
    private final List<ModuleInstanceDef> modules = new ArrayList<>();
    private final List<LookupTableDef> lookupTables = new ArrayList<>();
    private final List<CldVariableDef> cldVariables = new ArrayList<>();
    private final List<CausalLinkDef> causalLinks = new ArrayList<>();
    private final List<ReferenceDataset> referenceDatasets = new ArrayList<>();
    private final Set<String> nameIndex = new HashSet<>();
    private final List<ModelEditListener> listeners = new CopyOnWriteArrayList<>();
    private final EquationReferenceManager equationRefManager =
            new EquationReferenceManager(flows, auxiliaries);
    private SimulationSettings simulationSettings;
    private int nextStockId = 1;
    private int nextFlowId = 1;
    private int nextAuxId = 1;
    private int nextModuleId = 1;
    private int nextLookupId = 1;
    private int nextCldVariableId = 1;

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
        for (ModelEditListener l : listeners) {
            l.onElementAdded(name, typeName);
        }
    }

    private void fireElementRemoved(String name) {
        for (ModelEditListener l : listeners) {
            l.onElementRemoved(name);
        }
    }

    private void fireElementRenamed(String oldName, String newName) {
        for (ModelEditListener l : listeners) {
            l.onElementRenamed(oldName, newName);
        }
    }

    private void fireEquationChanged(String elementName) {
        for (ModelEditListener l : listeners) {
            l.onEquationChanged(elementName);
        }
    }

    /**
     * Loads all elements from an immutable {@link ModelDefinition} into mutable lists,
     * clearing any previous state.
     */
    public void loadFrom(ModelDefinition definition) {
        checkFxThread();
        modelName = definition.name();
        modelComment = definition.comment() != null ? definition.comment() : "";
        stocks.clear();
        flows.clear();
        auxiliaries.clear();
        modules.clear();
        lookupTables.clear();
        cldVariables.clear();
        causalLinks.clear();
        referenceDatasets.clear();
        nameIndex.clear();

        stocks.addAll(definition.stocks());
        flows.addAll(definition.flows());
        auxiliaries.addAll(definition.auxiliaries());
        modules.addAll(definition.modules());
        lookupTables.addAll(definition.lookupTables());
        cldVariables.addAll(definition.cldVariables());
        causalLinks.addAll(definition.causalLinks());
        referenceDatasets.addAll(definition.referenceDatasets());
        simulationSettings = definition.defaultSimulation();
        metadata = definition.metadata();

        stocks.forEach(s -> nameIndex.add(s.name()));
        flows.forEach(f -> nameIndex.add(f.name()));
        auxiliaries.forEach(a -> nameIndex.add(a.name()));
        modules.forEach(m -> nameIndex.add(m.instanceName()));
        lookupTables.forEach(lt -> nameIndex.add(lt.name()));
        cldVariables.forEach(v -> nameIndex.add(v.name()));

        // Set per-type counters past any existing numeric suffix
        nextStockId = maxIdFrom(stocks.stream().map(StockDef::name), "Stock ");
        nextFlowId = maxIdFrom(flows.stream().map(FlowDef::name), "Flow ");
        nextAuxId = maxIdFrom(auxiliaries.stream().map(AuxDef::name), "Aux ");
        nextModuleId = maxIdFrom(modules.stream().map(ModuleInstanceDef::instanceName), "Module ");
        nextLookupId = maxIdFrom(lookupTables.stream().map(LookupTableDef::name), "Lookup ");
        nextCldVariableId = maxIdFrom(cldVariables.stream().map(CldVariableDef::name), "Variable ");
    }

    private static int maxIdFrom(java.util.stream.Stream<String> names, String prefix) {
        int[] max = {0};
        names.forEach(name -> {
            if (name.startsWith(prefix)) {
                try {
                    int num = Integer.parseInt(name.substring(prefix.length()));
                    if (num > max[0]) {
                        max[0] = num;
                    }
                } catch (NumberFormatException ex) {
                    log.trace("Not an auto-named element: '{}'", name, ex);
                }
            }
        });
        return max[0] + 1;
    }

    /**
     * Adds a new stock with an auto-generated name.
     * @return the name of the created stock
     */
    public String addStock() {
        checkFxThread();
        String name = "Stock " + nextStockId++;
        stocks.add(new StockDef(name, 0, "units"));
        nameIndex.add(name);
        fireElementAdded(name, "Stock");
        return name;
    }

    /**
     * Adds a new flow with an auto-generated name and cloud-to-cloud connections.
     * @return the name of the created flow
     */
    public String addFlow() {
        checkFxThread();
        return addFlow(null, null);
    }

    /**
     * Adds a new flow with an auto-generated name and the specified source/sink connections.
     * @param source the source stock name (null for a cloud source)
     * @param sink the sink stock name (null for a cloud sink)
     * @return the name of the created flow
     */
    public String addFlow(String source, String sink) {
        checkFxThread();
        String name = "Flow " + nextFlowId++;
        String materialUnit = inferMaterialUnit(source, sink);
        flows.add(new FlowDef(name, null, "0", "Day", materialUnit, source, sink, List.of()));
        nameIndex.add(name);
        fireElementAdded(name, "Flow");
        return name;
    }

    /**
     * Adds a new auxiliary with an auto-generated name.
     * @return the name of the created auxiliary
     */
    public String addAux() {
        checkFxThread();
        String name = "Aux " + nextAuxId++;
        auxiliaries.add(new AuxDef(name, "0", "units"));
        nameIndex.add(name);
        fireElementAdded(name, "Variable");
        return name;
    }

    /**
     * Adds a new stock copied from a template with an auto-generated name.
     * @return the name of the created stock
     */
    public String addStockFrom(StockDef template) {
        checkFxThread();
        String name = resolveUniqueName(template.name(), "Stock ", nextStockId, nameIndex);
        if (name.startsWith("Stock ")) {
            nextStockId = parseIdSuffix(name, "Stock ") + 1;
        }
        stocks.add(new StockDef(name, template.comment(), template.initialValue(),
                template.unit(), template.negativeValuePolicy()));
        nameIndex.add(name);
        return name;
    }

    /**
     * Adds a new flow copied from a template with an auto-generated name
     * and the specified source/sink connections.
     * @return the name of the created flow
     */
    public String addFlowFrom(FlowDef template, String source, String sink) {
        checkFxThread();
        String name = resolveUniqueName(template.name(), "Flow ", nextFlowId, nameIndex);
        if (name.startsWith("Flow ")) {
            nextFlowId = parseIdSuffix(name, "Flow ") + 1;
        }
        String matUnit = template.materialUnit() != null
                ? template.materialUnit() : inferMaterialUnit(source, sink);
        flows.add(new FlowDef(name, template.comment(), template.equation(),
                template.timeUnit(), matUnit, source, sink, List.of()));
        nameIndex.add(name);
        return name;
    }

    /**
     * Adds a new auxiliary copied from a template with an auto-generated name
     * and the specified equation.
     * @return the name of the created auxiliary
     */
    public String addAuxFrom(AuxDef template, String equation) {
        checkFxThread();
        String name = resolveUniqueName(template.name(), "Aux ", nextAuxId, nameIndex);
        if (name.startsWith("Aux ")) {
            nextAuxId = parseIdSuffix(name, "Aux ") + 1;
        }
        auxiliaries.add(new AuxDef(name, template.comment(), equation, template.unit()));
        nameIndex.add(name);
        return name;
    }

    /**
     * Adds a new module instance copied from a template with an auto-generated name.
     * @return the instance name of the created module
     */
    public String addModuleFrom(ModuleInstanceDef template) {
        checkFxThread();
        String name = resolveUniqueName(template.instanceName(), "Module ", nextModuleId, nameIndex);
        if (name.startsWith("Module ")) {
            nextModuleId = parseIdSuffix(name, "Module ") + 1;
        }
        modules.add(new ModuleInstanceDef(name, template.definition(),
                template.inputBindings(), template.outputBindings()));
        nameIndex.add(name);
        return name;
    }

    /**
     * Adds a new module instance with an auto-generated name and empty definition.
     * @return the instance name of the created module
     */
    public String addModule() {
        checkFxThread();
        String name = "Module " + nextModuleId++;
        ModelDefinition emptyDef = new ModelDefinition(
                name, null, null,
                List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of(), null);
        modules.add(new ModuleInstanceDef(name, emptyDef, Map.of(), Map.of()));
        nameIndex.add(name);
        fireElementAdded(name, "Module");
        return name;
    }

    /**
     * Adds a new lookup table with an auto-generated name and default data points.
     * @return the name of the created lookup table
     */
    public String addLookup() {
        checkFxThread();
        String name = "Lookup " + nextLookupId++;
        lookupTables.add(new LookupTableDef(name,
                new double[]{0.0, 1.0}, new double[]{0.0, 1.0}, "LINEAR"));
        nameIndex.add(name);
        fireElementAdded(name, "Lookup");
        return name;
    }

    /**
     * Adds a new lookup table copied from a template with an auto-generated name.
     * @return the name of the created lookup table
     */
    public String addLookupFrom(LookupTableDef template) {
        checkFxThread();
        String name = resolveUniqueName(template.name(), "Lookup ", nextLookupId, nameIndex);
        if (name.startsWith("Lookup ")) {
            nextLookupId = parseIdSuffix(name, "Lookup ") + 1;
        }
        lookupTables.add(new LookupTableDef(name, template.comment(),
                template.xValues(), template.yValues(), template.interpolation()));
        nameIndex.add(name);
        return name;
    }

    /**
     * Returns the lookup table with the given name.
     */
    public Optional<LookupTableDef> getLookupTableByName(String name) {
        return findByName(lookupTables, name, LookupTableDef::name);
    }

    /**
     * Replaces the lookup table definition for the given name.
     * @return true if the lookup table was found and updated
     */
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

    /**
     * Removes the element with the given name from the appropriate list.
     * If a stock is removed, any flow referencing it as source or sink has
     * that connection nullified (becomes a cloud).
     */
    public void removeElement(String name) {
        checkFxThread();
        nameIndex.remove(name);
        boolean wasStock = stocks.removeIf(s -> s.name().equals(name));

        if (wasStock) {
            // Nullify flow source/sink references to the deleted stock
            for (int i = 0; i < flows.size(); i++) {
                FlowDef f = flows.get(i);
                boolean sourceMatch = name.equals(f.source());
                boolean sinkMatch = name.equals(f.sink());
                if (sourceMatch || sinkMatch) {
                    flows.set(i, new FlowDef(
                            f.name(),
                            f.comment(),
                            f.equation(),
                            f.timeUnit(),
                            f.materialUnit(),
                            sourceMatch ? null : f.source(),
                            sinkMatch ? null : f.sink(),
                            f.subscripts()
                    ));
                }
            }
        }

        if (!wasStock) {
            if (flows.removeIf(f -> f.name().equals(name))) {
                // flow removed — fall through to clean equations
            } else if (auxiliaries.removeIf(a -> a.name().equals(name))) {
                // aux removed — fall through to clean equations
            } else {
                if (!lookupTables.removeIf(lt -> lt.name().equals(name))) {
                    if (!modules.removeIf(m -> m.instanceName().equals(name))) {
                        cldVariables.removeIf(v -> v.name().equals(name));
                    }
                }
            }
        }

        // Remove causal links referencing the deleted element
        causalLinks.removeIf(link -> link.from().equals(name) || link.to().equals(name));

        // Clean equation references: replace deleted element's token with "0"
        String deletedToken = name.replace(' ', '_');
        equationRefManager.updateEquationReferences(deletedToken, "0");

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
        if (oldName == null || newName == null || oldName.equals(newName)) {
            return false;
        }

        // Reject if newName is already in use by any element
        if (hasElement(newName)) {
            return false;
        }

        boolean found = renameInList(stocks, oldName, newName, StockDef::name,
                (s, n) -> new StockDef(n, s.comment(), s.initialValue(),
                        s.unit(), s.negativeValuePolicy()))
                || renameInList(flows, oldName, newName, FlowDef::name,
                (f, n) -> new FlowDef(n, f.comment(), f.equation(),
                        f.timeUnit(), f.materialUnit(), f.source(), f.sink(), f.subscripts()))
                || renameInList(auxiliaries, oldName, newName, AuxDef::name,
                (a, n) -> new AuxDef(n, a.comment(), a.equation(), a.unit()))
                || renameInList(modules, oldName, newName, ModuleInstanceDef::instanceName,
                (m, n) -> new ModuleInstanceDef(n, m.definition(),
                        m.inputBindings(), m.outputBindings()))
                || renameInList(lookupTables, oldName, newName, LookupTableDef::name,
                (lt, n) -> new LookupTableDef(n, lt.comment(),
                        lt.xValues(), lt.yValues(), lt.interpolation()))
                || renameInList(cldVariables, oldName, newName, CldVariableDef::name,
                (v, n) -> new CldVariableDef(n, v.comment()));

        if (!found) {
            return false;
        }

        nameIndex.remove(oldName);
        nameIndex.add(newName);

        // Update causal link references
        for (int i = 0; i < causalLinks.size(); i++) {
            CausalLinkDef link = causalLinks.get(i);
            boolean fromMatch = oldName.equals(link.from());
            boolean toMatch = oldName.equals(link.to());
            if (fromMatch || toMatch) {
                causalLinks.set(i, new CausalLinkDef(
                        fromMatch ? newName : link.from(),
                        toMatch ? newName : link.to(),
                        link.polarity(),
                        link.comment()));
            }
        }

        // Update flow source/sink references
        for (int i = 0; i < flows.size(); i++) {
            FlowDef f = flows.get(i);
            boolean sourceMatch = oldName.equals(f.source());
            boolean sinkMatch = oldName.equals(f.sink());
            if (sourceMatch || sinkMatch) {
                flows.set(i, new FlowDef(
                        f.name(), f.comment(), f.equation(), f.timeUnit(),
                        f.materialUnit(),
                        sourceMatch ? newName : f.source(),
                        sinkMatch ? newName : f.sink(),
                        f.subscripts()));
            }
        }

        // Update module input/output bindings that reference the old name
        for (int i = 0; i < modules.size(); i++) {
            ModuleInstanceDef m = modules.get(i);
            boolean changed = false;
            Map<String, String> newInputs = new java.util.LinkedHashMap<>(m.inputBindings());
            for (Map.Entry<String, String> entry : newInputs.entrySet()) {
                if (oldName.equals(entry.getValue())) {
                    entry.setValue(newName);
                    changed = true;
                }
            }
            Map<String, String> newOutputs = new java.util.LinkedHashMap<>(m.outputBindings());
            for (Map.Entry<String, String> entry : newOutputs.entrySet()) {
                if (oldName.equals(entry.getValue())) {
                    entry.setValue(newName);
                    changed = true;
                }
            }
            if (changed) {
                modules.set(i, new ModuleInstanceDef(
                        m.instanceName(), m.definition(), newInputs, newOutputs));
            }
        }

        // Update equation references (underscore convention)
        String oldToken = oldName.replace(' ', '_');
        String newToken = newName.replace(' ', '_');
        equationRefManager.updateEquationReferences(oldToken, newToken);

        fireElementRenamed(oldName, newName);
        return true;
    }

    /**
     * Reconnects a flow endpoint to a different stock, or disconnects it (pass null for stockName).
     *
     * @param flowName the flow to modify
     * @param end which end (SOURCE or SINK) to reconnect
     * @param stockName the new stock to connect to, or null to disconnect (cloud)
     * @return true if the flow was found and updated
     */
    public boolean reconnectFlow(String flowName, FlowEndpointCalculator.FlowEnd end,
                                 String stockName) {
        checkFxThread();
        // Validate: if a stock name is given, it must actually exist
        if (stockName != null && !hasElement(stockName)) {
            return false;
        }

        for (int i = 0; i < flows.size(); i++) {
            if (flows.get(i).name().equals(flowName)) {
                FlowDef f = flows.get(i);

                // Prevent self-loop: stockName must not equal the opposite endpoint
                if (stockName != null) {
                    String opposite = (end == FlowEndpointCalculator.FlowEnd.SOURCE)
                            ? f.sink() : f.source();
                    if (stockName.equals(opposite)) {
                        return false;
                    }
                }

                if (end == FlowEndpointCalculator.FlowEnd.SOURCE) {
                    flows.set(i, new FlowDef(f.name(), f.comment(), f.equation(),
                            f.timeUnit(), f.materialUnit(), stockName, f.sink(), f.subscripts()));
                } else {
                    flows.set(i, new FlowDef(f.name(), f.comment(), f.equation(),
                            f.timeUnit(), f.materialUnit(), f.source(), stockName, f.subscripts()));
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Sets the equation of a flow.
     *
     * @return true if the flow was found and updated
     */
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

    /**
     * Sets the equation of an auxiliary.
     *
     * @return true if the auxiliary was found and updated
     */
    public boolean setAuxEquation(String name, String equation) {
        checkFxThread();
        if (equation == null || equation.isBlank()) {
            return false;
        }
        boolean updated = updateInList(auxiliaries, name, AuxDef::name,
                a -> new AuxDef(a.name(), a.comment(), equation, a.unit()));
        if (updated) {
            fireEquationChanged(name);
        }
        return updated;
    }

    /**
     * Sets the initial value of a stock.
     *
     * @return true if the stock was found and updated
     */
    public boolean setStockInitialValue(String name, double value) {
        checkFxThread();
        return updateInList(stocks, name, StockDef::name,
                s -> new StockDef(name, s.comment(), value,
                        s.unit(), s.negativeValuePolicy()));
    }

    /**
     * Sets the unit of a stock.
     *
     * @return true if the stock was found and updated
     */
    public boolean setStockUnit(String name, String unit) {
        checkFxThread();
        if (unit == null) {
            return false;
        }
        return updateInList(stocks, name, StockDef::name,
                s -> new StockDef(name, s.comment(), s.initialValue(),
                        unit, s.negativeValuePolicy()));
    }

    /**
     * Sets the negative value policy of a stock.
     *
     * @return true if the stock was found and updated
     */
    public boolean setStockNegativeValuePolicy(String name, String policy) {
        checkFxThread();
        return updateInList(stocks, name, StockDef::name,
                s -> new StockDef(name, s.comment(), s.initialValue(),
                        s.unit(), policy));
    }

    /**
     * Sets the time unit of a flow.
     *
     * @return true if the flow was found and updated
     */
    public boolean setFlowTimeUnit(String name, String timeUnit) {
        checkFxThread();
        if (timeUnit == null || timeUnit.isBlank()) {
            return false;
        }
        return updateInList(flows, name, FlowDef::name,
                f -> new FlowDef(f.name(), f.comment(), f.equation(),
                        timeUnit, f.materialUnit(), f.source(), f.sink(), f.subscripts()));
    }

    /**
     * Sets the material unit of a flow.
     *
     * @return true if the flow was found and updated
     */
    public boolean setFlowMaterialUnit(String name, String materialUnit) {
        checkFxThread();
        return updateInList(flows, name, FlowDef::name,
                f -> new FlowDef(f.name(), f.comment(), f.equation(),
                        f.timeUnit(), materialUnit, f.source(), f.sink(), f.subscripts()));
    }

    /**
     * Sets the unit of an auxiliary.
     *
     * @return true if the auxiliary was found and updated
     */
    public boolean setAuxUnit(String name, String unit) {
        checkFxThread();
        if (unit == null) {
            return false;
        }
        return updateInList(auxiliaries, name, AuxDef::name,
                a -> new AuxDef(a.name(), a.comment(), a.equation(), unit));
    }

    /**
     * Sets the comment of a stock.
     *
     * @return true if the stock was found and updated
     */
    public boolean setStockComment(String name, String comment) {
        checkFxThread();
        return updateInList(stocks, name, StockDef::name,
                s -> new StockDef(name, comment, s.initialValue(),
                        s.unit(), s.negativeValuePolicy()));
    }

    /**
     * Sets the comment of a flow.
     *
     * @return true if the flow was found and updated
     */
    public boolean setFlowComment(String name, String comment) {
        checkFxThread();
        return updateInList(flows, name, FlowDef::name,
                f -> new FlowDef(f.name(), comment, f.equation(),
                        f.timeUnit(), f.materialUnit(), f.source(), f.sink(), f.subscripts()));
    }

    /**
     * Sets the comment of an auxiliary.
     *
     * @return true if the auxiliary was found and updated
     */
    public boolean setAuxComment(String name, String comment) {
        checkFxThread();
        return updateInList(auxiliaries, name, AuxDef::name,
                a -> new AuxDef(a.name(), comment, a.equation(), a.unit()));
    }

    /**
     * Sets the comment of a lookup table.
     *
     * @return true if the lookup table was found and updated
     */
    public boolean setLookupComment(String name, String comment) {
        checkFxThread();
        return updateInList(lookupTables, name, LookupTableDef::name,
                lt -> new LookupTableDef(name, comment,
                        lt.xValues(), lt.yValues(), lt.interpolation()));
    }

    private <T> boolean renameInList(List<T> list, String oldName, String newName,
                                      Function<T, String> nameGetter,
                                      BiFunction<T, String, T> renamer) {
        return updateInList(list, oldName, nameGetter, item -> renamer.apply(item, newName));
    }

    /**
     * Finds the element with the given name in the list and replaces it with the
     * result of applying the updater function. Returns true if the element was found.
     */
    private <T> boolean updateInList(List<T> list, String name,
                                      Function<T, String> nameGetter,
                                      UnaryOperator<T> updater) {
        for (int i = 0; i < list.size(); i++) {
            if (nameGetter.apply(list.get(i)).equals(name)) {
                list.set(i, updater.apply(list.get(i)));
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the first element in the list whose name matches.
     */
    private <T> Optional<T> findByName(List<T> list, String name, Function<T, String> nameGetter) {
        for (T item : list) {
            if (nameGetter.apply(item).equals(name)) {
                return Optional.of(item);
            }
        }
        return Optional.empty();
    }

    /**
     * Infers a material unit from the connected stock(s). Checks sink first, then source.
     * Returns null if neither endpoint is a known stock.
     */
    private String inferMaterialUnit(String source, String sink) {
        if (sink != null) {
            Optional<StockDef> sinkStock = getStockByName(sink);
            if (sinkStock.isPresent()) {
                return sinkStock.get().unit();
            }
        }
        if (source != null) {
            Optional<StockDef> sourceStock = getStockByName(source);
            if (sourceStock.isPresent()) {
                return sourceStock.get().unit();
            }
        }
        return null;
    }

    // Equation reference management delegated to EquationReferenceManager

    /**
     * Returns true if any element (stock, flow, aux, constant, or module) has the given name.
     * Uses an O(1) hash set lookup instead of scanning all five element lists.
     */
    public boolean hasElement(String name) {
        return nameIndex.contains(name);
    }

    static final int MAX_NAME_LENGTH = ElementNameValidator.MAX_NAME_LENGTH;

    /**
     * Returns true if the given name is valid for an element identifier.
     * Delegates to {@link ElementNameValidator#isValidName(String)}.
     */
    public static boolean isValidName(String name) {
        return ElementNameValidator.isValidName(name);
    }

    public SimulationSettings getSimulationSettings() {
        return simulationSettings;
    }

    public void setSimulationSettings(SimulationSettings simulationSettings) {
        checkFxThread();
        this.simulationSettings = simulationSettings;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String name) {
        checkFxThread();
        if (name != null && !name.isBlank()) {
            modelName = name;
        }
    }

    public String getModelComment() {
        return modelComment;
    }

    public void setModelComment(String comment) {
        checkFxThread();
        modelComment = comment != null ? comment : "";
    }

    public ModelMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(ModelMetadata metadata) {
        checkFxThread();
        this.metadata = metadata;
    }

    public List<StockDef> getStocks() {
        return Collections.unmodifiableList(stocks);
    }

    public List<FlowDef> getFlows() {
        return Collections.unmodifiableList(flows);
    }

    public List<AuxDef> getAuxiliaries() {
        return Collections.unmodifiableList(auxiliaries);
    }

    /**
     * Returns the names of literal-valued auxiliaries (tunable parameters).
     */
    public List<String> getParameterNames() {
        return auxiliaries.stream()
                .filter(AuxDef::isLiteral)
                .map(AuxDef::name)
                .toList();
    }

    /**
     * Returns the stock with the given name.
     */
    public Optional<StockDef> getStockByName(String name) {
        return findByName(stocks, name, StockDef::name);
    }

    /**
     * Returns the flow with the given name.
     */
    public Optional<FlowDef> getFlowByName(String name) {
        return findByName(flows, name, FlowDef::name);
    }

    /**
     * Returns the auxiliary with the given name.
     */
    public Optional<AuxDef> getAuxByName(String name) {
        return findByName(auxiliaries, name, AuxDef::name);
    }

    public Optional<String> getStockUnit(String name) {
        return findByName(stocks, name, StockDef::name).map(StockDef::unit);
    }

    public Optional<String> getFlowEquation(String name) {
        return findByName(flows, name, FlowDef::name).map(FlowDef::equation);
    }

    public Optional<String> getAuxEquation(String name) {
        return findByName(auxiliaries, name, AuxDef::name).map(AuxDef::equation);
    }

    public List<ModuleInstanceDef> getModules() {
        return Collections.unmodifiableList(modules);
    }

    /**
     * Returns the module instance with the given name.
     */
    public Optional<ModuleInstanceDef> getModuleByName(String name) {
        return findByName(modules, name, ModuleInstanceDef::instanceName);
    }

    /**
     * Returns the index of the module with the given name, or -1 if not found.
     */
    public int getModuleIndex(String name) {
        for (int i = 0; i < modules.size(); i++) {
            if (modules.get(i).instanceName().equals(name)) {
                return i;
            }
        }
        return -1;
    }

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
     * Existing bindings for ports that still exist are preserved; bindings for
     * removed ports are dropped.
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

    public List<LookupTableDef> getLookupTables() {
        return Collections.unmodifiableList(lookupTables);
    }

    // === CLD Variables and Causal Links ===

    /**
     * Adds a new CLD variable with an auto-generated name.
     * @return the name of the created variable
     */
    public String addCldVariable() {
        checkFxThread();
        String name = resolveUniqueName("Variable " + nextCldVariableId,
                "Variable ", nextCldVariableId, nameIndex);
        nextCldVariableId = parseIdSuffix(name, "Variable ") + 1;
        cldVariables.add(new CldVariableDef(name));
        nameIndex.add(name);
        fireElementAdded(name, "CLD Variable");
        return name;
    }

    /**
     * Adds a CLD variable copied from a template.
     * @return the name of the created variable
     */
    public String addCldVariableFrom(CldVariableDef template) {
        checkFxThread();
        String name = resolveUniqueName(template.name(), "Variable ", nextCldVariableId, nameIndex);
        if (name.startsWith("Variable ")) {
            nextCldVariableId = parseIdSuffix(name, "Variable ") + 1;
        }
        cldVariables.add(new CldVariableDef(name, template.comment()));
        nameIndex.add(name);
        fireElementAdded(name, "CLD Variable");
        return name;
    }

    /**
     * Sets the comment on a CLD variable.
     * @return true if the variable was found and updated
     */
    public boolean setCldVariableComment(String name, String comment) {
        checkFxThread();
        return updateInList(cldVariables, name, CldVariableDef::name,
                v -> new CldVariableDef(name, comment));
    }

    public List<CldVariableDef> getCldVariables() {
        return Collections.unmodifiableList(cldVariables);
    }

    public CldVariableDef getCldVariableByName(String name) {
        for (CldVariableDef v : cldVariables) {
            if (v.name().equals(name)) {
                return v;
            }
        }
        return null;
    }

    /**
     * Adds a causal link between two elements.
     * @return true if both endpoints exist and the link was added
     */
    public boolean addCausalLink(String from, String to, CausalLinkDef.Polarity polarity) {
        checkFxThread();
        if (!hasElement(from) || !hasElement(to)) {
            return false;
        }
        for (CausalLinkDef existing : causalLinks) {
            if (existing.from().equals(from) && existing.to().equals(to)) {
                return false;
            }
        }
        causalLinks.add(new CausalLinkDef(from, to, polarity));
        return true;
    }

    /**
     * Removes a causal link between two elements.
     * @return true if the link was found and removed
     */
    public boolean removeCausalLink(String from, String to) {
        checkFxThread();
        return causalLinks.removeIf(link -> link.from().equals(from) && link.to().equals(to));
    }

    /**
     * Sets the polarity of an existing causal link.
     * @return true if the link was found and updated
     */
    public boolean setCausalLinkPolarity(String from, String to, CausalLinkDef.Polarity polarity) {
        checkFxThread();
        for (int i = 0; i < causalLinks.size(); i++) {
            CausalLinkDef link = causalLinks.get(i);
            if (link.from().equals(from) && link.to().equals(to)) {
                causalLinks.set(i, new CausalLinkDef(from, to, polarity, link.comment()));
                return true;
            }
        }
        return false;
    }

    public List<CausalLinkDef> getCausalLinks() {
        return Collections.unmodifiableList(causalLinks);
    }

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

        CldVariableDef variable = getCldVariableByName(name);
        if (variable == null) {
            return false;
        }

        // Remove the CLD variable
        cldVariables.removeIf(v -> v.name().equals(name));

        // Create the target S&F element with the same name
        switch (targetType) {
            case STOCK -> stocks.add(new StockDef(name, variable.comment(), 0, "units", null));
            case FLOW -> flows.add(new FlowDef(name, variable.comment(), "0", "Day", null, null));
            case AUX -> auxiliaries.add(new AuxDef(name, variable.comment(), "0", "units"));
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
     * Rebuilds an immutable {@link ModelDefinition} snapshot from the current editor state.
     */
    public ModelDefinition toModelDefinition() {
        return toModelDefinition(null);
    }

    /**
     * Rebuilds an immutable {@link ModelDefinition} snapshot including the given view layout.
     *
     * @param view the canvas layout to include, or null to omit view data
     */
    public ModelDefinition toModelDefinition(ViewDef view) {
        return new ModelDefinition(
                modelName,
                modelComment.isBlank() ? null : modelComment,
                null,
                List.copyOf(stocks),
                List.copyOf(flows),
                List.copyOf(auxiliaries),
                List.copyOf(lookupTables),
                List.copyOf(modules),
                List.of(),
                List.copyOf(cldVariables),
                List.copyOf(causalLinks),
                view != null ? List.of(view) : List.of(),
                simulationSettings,
                metadata,
                List.copyOf(referenceDatasets)
        );
    }

    /**
     * Adds a reference dataset for model validation overlay.
     */
    public void addReferenceDataset(ReferenceDataset dataset) {
        checkFxThread();
        referenceDatasets.add(dataset);
    }

    /**
     * Returns an unmodifiable view of the reference datasets.
     */
    public List<ReferenceDataset> getReferenceDatasets() {
        return Collections.unmodifiableList(referenceDatasets);
    }

    /**
     * Removes a single equation reference that creates the info link from {@code fromName}
     * to {@code toName}. The reference token is replaced with "0" in the target element's
     * equation only (not globally). Material flow connections are not affected.
     *
     * @return true if a reference was found and removed
     */
    public boolean removeConnectionReference(String fromName, String toName) {
        checkFxThread();
        String fromToken = fromName.replace(' ', '_');
        return equationRefManager.updateEquationByName(toName,
                eq -> replaceToken(eq, fromToken, "0"));
    }

    /**
     * Reroutes the source (from) end of an info link: in the target's equation,
     * replaces the old source token with the new source token.
     *
     * @return true if the equation was updated
     */
    public boolean rerouteConnectionSource(String oldFrom, String newFrom, String to) {
        checkFxThread();
        String oldToken = oldFrom.replace(' ', '_');
        String newToken = newFrom.replace(' ', '_');
        return equationRefManager.updateEquationByName(to,
                eq -> replaceToken(eq, oldToken, newToken));
    }

    /**
     * Reroutes the target (to) end of an info link: removes the source reference
     * from the old target's equation and adds it to the new target's equation.
     *
     * @return true if the reroute was performed
     */
    public boolean rerouteConnectionTarget(String from, String oldTo, String newTo) {
        checkFxThread();
        String fromToken = from.replace(' ', '_');

        // Remove reference from old target
        removeConnectionReference(from, oldTo);

        // Add reference to new target's equation
        return equationRefManager.addConnectionReference(newTo, fromToken);
    }

    /**
     * Generates connector routes from the current model state's dependency graph.
     */
    public List<ConnectorRoute> generateConnectors() {
        return ConnectorGenerator.generate(toModelDefinition());
    }
}

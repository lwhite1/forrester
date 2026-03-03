package com.deathrayresearch.forrester.app.canvas;

import com.deathrayresearch.forrester.model.def.AuxDef;
import com.deathrayresearch.forrester.model.def.ConnectorRoute;
import com.deathrayresearch.forrester.model.def.ConstantDef;
import com.deathrayresearch.forrester.model.def.FlowDef;
import com.deathrayresearch.forrester.model.def.LookupTableDef;
import com.deathrayresearch.forrester.model.def.ModelDefinition;
import com.deathrayresearch.forrester.model.def.ModuleInstanceDef;
import com.deathrayresearch.forrester.model.def.SimulationSettings;
import com.deathrayresearch.forrester.model.def.StockDef;
import com.deathrayresearch.forrester.model.def.ViewDef;
import com.deathrayresearch.forrester.model.graph.ConnectorGenerator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * Mutable model editing layer that sits between the UI and the engine's immutable
 * {@link ModelDefinition}. Supports adding and removing elements while preserving
 * the engine's immutability contract — an immutable snapshot can be rebuilt on demand
 * via {@link #toModelDefinition()}.
 */
public class ModelEditor {

    private String modelName = "Untitled";
    private final List<StockDef> stocks = new ArrayList<>();
    private final List<FlowDef> flows = new ArrayList<>();
    private final List<AuxDef> auxiliaries = new ArrayList<>();
    private final List<ConstantDef> constants = new ArrayList<>();
    private final List<ModuleInstanceDef> modules = new ArrayList<>();
    private final List<LookupTableDef> lookupTables = new ArrayList<>();
    private final Set<String> nameIndex = new HashSet<>();
    private SimulationSettings simulationSettings;
    private int nextId = 1;

    /**
     * Loads all elements from an immutable {@link ModelDefinition} into mutable lists,
     * clearing any previous state.
     */
    public void loadFrom(ModelDefinition definition) {
        modelName = definition.name();
        stocks.clear();
        flows.clear();
        auxiliaries.clear();
        constants.clear();
        modules.clear();
        lookupTables.clear();
        nameIndex.clear();

        stocks.addAll(definition.stocks());
        flows.addAll(definition.flows());
        auxiliaries.addAll(definition.auxiliaries());
        constants.addAll(definition.constants());
        modules.addAll(definition.modules());
        lookupTables.addAll(definition.lookupTables());
        simulationSettings = definition.defaultSimulation();

        stocks.forEach(s -> nameIndex.add(s.name()));
        flows.forEach(f -> nameIndex.add(f.name()));
        auxiliaries.forEach(a -> nameIndex.add(a.name()));
        constants.forEach(c -> nameIndex.add(c.name()));
        modules.forEach(m -> nameIndex.add(m.instanceName()));
        lookupTables.forEach(lt -> nameIndex.add(lt.name()));

        // Set nextId past any existing numeric suffix
        nextId = 1;
        updateNextId(stocks.stream().map(StockDef::name));
        updateNextId(flows.stream().map(FlowDef::name));
        updateNextId(auxiliaries.stream().map(AuxDef::name));
        updateNextId(constants.stream().map(ConstantDef::name));
        updateNextId(modules.stream().map(ModuleInstanceDef::instanceName));
        updateNextId(lookupTables.stream().map(LookupTableDef::name));
    }

    private void updateNextId(java.util.stream.Stream<String> names) {
        names.forEach(name -> {
            // Try to extract trailing number from auto-generated names like "Stock 3"
            int spaceIdx = name.lastIndexOf(' ');
            if (spaceIdx >= 0) {
                try {
                    int num = Integer.parseInt(name.substring(spaceIdx + 1));
                    if (num >= nextId) {
                        nextId = num + 1;
                    }
                } catch (NumberFormatException ignored) {
                    // Not an auto-named element
                }
            }
        });
    }

    /**
     * Adds a new stock with an auto-generated name.
     * @return the name of the created stock
     */
    public String addStock() {
        String name = "Stock " + nextId++;
        stocks.add(new StockDef(name, 0, "units"));
        nameIndex.add(name);
        return name;
    }

    /**
     * Adds a new flow with an auto-generated name and cloud-to-cloud connections.
     * @return the name of the created flow
     */
    public String addFlow() {
        return addFlow(null, null);
    }

    /**
     * Adds a new flow with an auto-generated name and the specified source/sink connections.
     * @param source the source stock name (null for a cloud source)
     * @param sink the sink stock name (null for a cloud sink)
     * @return the name of the created flow
     */
    public String addFlow(String source, String sink) {
        String name = "Flow " + nextId++;
        flows.add(new FlowDef(name, "0", "day", source, sink));
        nameIndex.add(name);
        return name;
    }

    /**
     * Adds a new auxiliary with an auto-generated name.
     * @return the name of the created auxiliary
     */
    public String addAux() {
        String name = "Aux " + nextId++;
        auxiliaries.add(new AuxDef(name, "0", "units"));
        nameIndex.add(name);
        return name;
    }

    /**
     * Adds a new constant with an auto-generated name.
     * @return the name of the created constant
     */
    public String addConstant() {
        String name = "Constant " + nextId++;
        constants.add(new ConstantDef(name, 0, "units"));
        nameIndex.add(name);
        return name;
    }

    /**
     * Adds a new stock copied from a template with an auto-generated name.
     * @return the name of the created stock
     */
    public String addStockFrom(StockDef template) {
        String name = "Stock " + nextId++;
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
        String name = "Flow " + nextId++;
        flows.add(new FlowDef(name, template.comment(), template.equation(),
                template.timeUnit(), source, sink));
        nameIndex.add(name);
        return name;
    }

    /**
     * Adds a new auxiliary copied from a template with an auto-generated name
     * and the specified equation.
     * @return the name of the created auxiliary
     */
    public String addAuxFrom(AuxDef template, String equation) {
        String name = "Aux " + nextId++;
        auxiliaries.add(new AuxDef(name, template.comment(), equation, template.unit()));
        nameIndex.add(name);
        return name;
    }

    /**
     * Adds a new constant copied from a template with an auto-generated name.
     * @return the name of the created constant
     */
    public String addConstantFrom(ConstantDef template) {
        String name = "Constant " + nextId++;
        constants.add(new ConstantDef(name, template.comment(), template.value(), template.unit()));
        nameIndex.add(name);
        return name;
    }

    /**
     * Adds a new module instance copied from a template with an auto-generated name.
     * @return the instance name of the created module
     */
    public String addModuleFrom(ModuleInstanceDef template) {
        String name = "Module " + nextId++;
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
        String name = "Module " + nextId++;
        ModelDefinition emptyDef = new ModelDefinition(
                name, null, null,
                List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of(), null);
        modules.add(new ModuleInstanceDef(name, emptyDef, Map.of(), Map.of()));
        nameIndex.add(name);
        return name;
    }

    /**
     * Adds a new lookup table with an auto-generated name and default data points.
     * @return the name of the created lookup table
     */
    public String addLookup() {
        String name = "Lookup " + nextId++;
        lookupTables.add(new LookupTableDef(name,
                new double[]{0.0, 1.0}, new double[]{0.0, 1.0}, "LINEAR"));
        nameIndex.add(name);
        return name;
    }

    /**
     * Adds a new lookup table copied from a template with an auto-generated name.
     * @return the name of the created lookup table
     */
    public String addLookupFrom(LookupTableDef template) {
        String name = "Lookup " + nextId++;
        lookupTables.add(new LookupTableDef(name, template.comment(),
                template.xValues(), template.yValues(), template.interpolation()));
        nameIndex.add(name);
        return name;
    }

    /**
     * Returns the lookup table with the given name, or null if not found.
     */
    public LookupTableDef getLookupTableByName(String name) {
        return findByName(lookupTables, name, LookupTableDef::name);
    }

    /**
     * Replaces the lookup table definition for the given name.
     * @return true if the lookup table was found and updated
     */
    public boolean setLookupTable(String name, LookupTableDef updated) {
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
                            sourceMatch ? null : f.source(),
                            sinkMatch ? null : f.sink()
                    ));
                }
            }
        }

        if (!wasStock) {
            if (flows.removeIf(f -> f.name().equals(name))) {
                // flow removed — fall through to clean equations
            } else if (auxiliaries.removeIf(a -> a.name().equals(name))) {
                // aux removed — fall through to clean equations
            } else if (!constants.removeIf(c -> c.name().equals(name))) {
                if (!lookupTables.removeIf(lt -> lt.name().equals(name))) {
                    modules.removeIf(m -> m.instanceName().equals(name));
                }
            }
        }

        // Clean equation references: replace deleted element's token with "0"
        String deletedToken = name.replace(' ', '_');
        updateEquationReferences(deletedToken, "0");
    }

    /**
     * Renames an element across all model data. Updates the element's own name in
     * the appropriate list, updates flow source/sink references, and updates equation
     * references using the underscore token convention.
     *
     * @return true if the element was found and renamed
     */
    public boolean renameElement(String oldName, String newName) {
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
                        f.timeUnit(), f.source(), f.sink()))
                || renameInList(auxiliaries, oldName, newName, AuxDef::name,
                (a, n) -> new AuxDef(n, a.comment(), a.equation(), a.unit()))
                || renameInList(constants, oldName, newName, ConstantDef::name,
                (c, n) -> new ConstantDef(n, c.comment(), c.value(), c.unit()))
                || renameInList(modules, oldName, newName, ModuleInstanceDef::instanceName,
                (m, n) -> new ModuleInstanceDef(n, m.definition(),
                        m.inputBindings(), m.outputBindings()))
                || renameInList(lookupTables, oldName, newName, LookupTableDef::name,
                (lt, n) -> new LookupTableDef(n, lt.comment(),
                        lt.xValues(), lt.yValues(), lt.interpolation()));

        if (!found) {
            return false;
        }

        nameIndex.remove(oldName);
        nameIndex.add(newName);

        // Update flow source/sink references
        for (int i = 0; i < flows.size(); i++) {
            FlowDef f = flows.get(i);
            boolean sourceMatch = oldName.equals(f.source());
            boolean sinkMatch = oldName.equals(f.sink());
            if (sourceMatch || sinkMatch) {
                flows.set(i, new FlowDef(
                        f.name(), f.comment(), f.equation(), f.timeUnit(),
                        sourceMatch ? newName : f.source(),
                        sinkMatch ? newName : f.sink()));
            }
        }

        // Update equation references (underscore convention)
        String oldToken = oldName.replace(' ', '_');
        String newToken = newName.replace(' ', '_');
        updateEquationReferences(oldToken, newToken);

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
                            f.timeUnit(), stockName, f.sink()));
                } else {
                    flows.set(i, new FlowDef(f.name(), f.comment(), f.equation(),
                            f.timeUnit(), f.source(), stockName));
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Sets the value of a constant.
     *
     * @return true if the constant was found and updated
     */
    public boolean setConstantValue(String name, double value) {
        return updateInList(constants, name, ConstantDef::name,
                c -> new ConstantDef(name, c.comment(), value, c.unit()));
    }

    /**
     * Sets the equation of a flow.
     *
     * @return true if the flow was found and updated
     */
    public boolean setFlowEquation(String name, String equation) {
        if (equation == null || equation.isBlank()) {
            return false;
        }
        return updateInList(flows, name, FlowDef::name,
                f -> new FlowDef(f.name(), f.comment(), equation,
                        f.timeUnit(), f.source(), f.sink()));
    }

    /**
     * Sets the equation of an auxiliary.
     *
     * @return true if the auxiliary was found and updated
     */
    public boolean setAuxEquation(String name, String equation) {
        if (equation == null || equation.isBlank()) {
            return false;
        }
        return updateInList(auxiliaries, name, AuxDef::name,
                a -> new AuxDef(a.name(), a.comment(), equation, a.unit()));
    }

    /**
     * Sets the initial value of a stock.
     *
     * @return true if the stock was found and updated
     */
    public boolean setStockInitialValue(String name, double value) {
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
        if (timeUnit == null || timeUnit.isBlank()) {
            return false;
        }
        return updateInList(flows, name, FlowDef::name,
                f -> new FlowDef(f.name(), f.comment(), f.equation(),
                        timeUnit, f.source(), f.sink()));
    }

    /**
     * Sets the unit of an auxiliary.
     *
     * @return true if the auxiliary was found and updated
     */
    public boolean setAuxUnit(String name, String unit) {
        if (unit == null) {
            return false;
        }
        return updateInList(auxiliaries, name, AuxDef::name,
                a -> new AuxDef(a.name(), a.comment(), a.equation(), unit));
    }

    /**
     * Sets the unit of a constant.
     *
     * @return true if the constant was found and updated
     */
    public boolean setConstantUnit(String name, String unit) {
        if (unit == null) {
            return false;
        }
        return updateInList(constants, name, ConstantDef::name,
                c -> new ConstantDef(name, c.comment(), c.value(), unit));
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
     * Returns the first element in the list whose name matches, or null if not found.
     */
    private <T> T findByName(List<T> list, String name, Function<T, String> nameGetter) {
        for (T item : list) {
            if (nameGetter.apply(item).equals(name)) {
                return item;
            }
        }
        return null;
    }

    /**
     * Finds the flow or auxiliary with the given name and applies a transform to its equation.
     * Returns true if the equation was actually changed.
     */
    private boolean updateEquationByName(String targetName, UnaryOperator<String> transform) {
        for (int i = 0; i < flows.size(); i++) {
            FlowDef f = flows.get(i);
            if (f.name().equals(targetName)) {
                String updated = transform.apply(f.equation());
                if (!updated.equals(f.equation())) {
                    flows.set(i, new FlowDef(f.name(), f.comment(), updated,
                            f.timeUnit(), f.source(), f.sink()));
                    return true;
                }
                return false;
            }
        }
        for (int i = 0; i < auxiliaries.size(); i++) {
            AuxDef a = auxiliaries.get(i);
            if (a.name().equals(targetName)) {
                String updated = transform.apply(a.equation());
                if (!updated.equals(a.equation())) {
                    auxiliaries.set(i, new AuxDef(a.name(), a.comment(), updated, a.unit()));
                    return true;
                }
                return false;
            }
        }
        return false;
    }

    private void updateEquationReferences(String oldToken, String newToken) {
        if (oldToken.equals(newToken)) {
            return;
        }
        for (int i = 0; i < flows.size(); i++) {
            FlowDef f = flows.get(i);
            String updated = replaceToken(f.equation(), oldToken, newToken);
            if (!updated.equals(f.equation())) {
                flows.set(i, new FlowDef(f.name(), f.comment(), updated,
                        f.timeUnit(), f.source(), f.sink()));
            }
        }
        for (int i = 0; i < auxiliaries.size(); i++) {
            AuxDef a = auxiliaries.get(i);
            String updated = replaceToken(a.equation(), oldToken, newToken);
            if (!updated.equals(a.equation())) {
                auxiliaries.set(i, new AuxDef(a.name(), a.comment(), updated, a.unit()));
            }
        }
    }

    /**
     * Word-boundary-aware token replacement in an equation string.
     * Tokens in equations use underscores for spaces (e.g. Contact_Rate).
     */
    static String replaceToken(String equation, String oldToken, String newToken) {
        StringBuilder result = new StringBuilder();
        int len = equation.length();
        int tokenLen = oldToken.length();
        int i = 0;

        while (i < len) {
            int idx = equation.indexOf(oldToken, i);
            if (idx < 0) {
                result.append(equation, i, len);
                break;
            }

            // Check word boundaries
            boolean startOk = idx == 0 || !isTokenChar(equation.charAt(idx - 1));
            boolean endOk = idx + tokenLen >= len || !isTokenChar(equation.charAt(idx + tokenLen));

            if (startOk && endOk) {
                result.append(equation, i, idx);
                result.append(newToken);
                i = idx + tokenLen;
            } else {
                result.append(equation, i, idx + 1);
                i = idx + 1;
            }
        }

        return result.toString();
    }

    static boolean isTokenChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    /**
     * Returns true if any element (stock, flow, aux, constant, or module) has the given name.
     * Uses an O(1) hash set lookup instead of scanning all five element lists.
     */
    public boolean hasElement(String name) {
        return nameIndex.contains(name);
    }

    /**
     * Returns true if the given name is valid for an element identifier.
     * A valid name is non-blank and contains only letters, digits, spaces, and underscores.
     */
    public static boolean isValidName(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (!Character.isLetterOrDigit(c) && c != ' ' && c != '_') {
                return false;
            }
        }
        return true;
    }

    public SimulationSettings getSimulationSettings() {
        return simulationSettings;
    }

    public void setSimulationSettings(SimulationSettings simulationSettings) {
        this.simulationSettings = simulationSettings;
    }

    public String getModelName() {
        return modelName;
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

    public List<ConstantDef> getConstants() {
        return Collections.unmodifiableList(constants);
    }

    public ConstantDef getConstantByName(String name) {
        return findByName(constants, name, ConstantDef::name);
    }

    /**
     * Returns the stock with the given name, or null if not found.
     */
    public StockDef getStockByName(String name) {
        return findByName(stocks, name, StockDef::name);
    }

    /**
     * Returns the flow with the given name, or null if not found.
     */
    public FlowDef getFlowByName(String name) {
        return findByName(flows, name, FlowDef::name);
    }

    /**
     * Returns the auxiliary with the given name, or null if not found.
     */
    public AuxDef getAuxByName(String name) {
        return findByName(auxiliaries, name, AuxDef::name);
    }

    public String getStockUnit(String name) {
        StockDef s = findByName(stocks, name, StockDef::name);
        return s != null ? s.unit() : null;
    }

    public String getFlowEquation(String name) {
        FlowDef f = findByName(flows, name, FlowDef::name);
        return f != null ? f.equation() : null;
    }

    public String getAuxEquation(String name) {
        AuxDef a = findByName(auxiliaries, name, AuxDef::name);
        return a != null ? a.equation() : null;
    }

    public List<ModuleInstanceDef> getModules() {
        return Collections.unmodifiableList(modules);
    }

    /**
     * Returns the module instance with the given name, or null if not found.
     */
    public ModuleInstanceDef getModuleByName(String name) {
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
        if (index < 0 || index >= modules.size()) {
            return;
        }
        ModuleInstanceDef existing = modules.get(index);
        modules.set(index, new ModuleInstanceDef(
                existing.instanceName(), newDef,
                existing.inputBindings(), existing.outputBindings()));
    }

    /**
     * Updates the input and output bindings of the module with the given name.
     *
     * @return true if the module was found and updated
     */
    public boolean updateModuleBindings(String name,
                                         Map<String, String> inputBindings,
                                         Map<String, String> outputBindings) {
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
                null,
                null,
                List.copyOf(stocks),
                List.copyOf(flows),
                List.copyOf(auxiliaries),
                List.copyOf(constants),
                List.copyOf(lookupTables),
                List.copyOf(modules),
                List.of(),
                view != null ? List.of(view) : List.of(),
                simulationSettings
        );
    }

    /**
     * Removes a single equation reference that creates the info link from {@code fromName}
     * to {@code toName}. The reference token is replaced with "0" in the target element's
     * equation only (not globally). Material flow connections are not affected.
     *
     * @return true if a reference was found and removed
     */
    public boolean removeConnectionReference(String fromName, String toName) {
        String fromToken = fromName.replace(' ', '_');
        return updateEquationByName(toName, eq -> replaceToken(eq, fromToken, "0"));
    }

    /**
     * Reroutes the source (from) end of an info link: in the target's equation,
     * replaces the old source token with the new source token.
     *
     * @return true if the equation was updated
     */
    public boolean rerouteConnectionSource(String oldFrom, String newFrom, String to) {
        String oldToken = oldFrom.replace(' ', '_');
        String newToken = newFrom.replace(' ', '_');
        return updateEquationByName(to, eq -> replaceToken(eq, oldToken, newToken));
    }

    /**
     * Reroutes the target (to) end of an info link: removes the source reference
     * from the old target's equation and adds it to the new target's equation.
     *
     * @return true if the reroute was performed
     */
    public boolean rerouteConnectionTarget(String from, String oldTo, String newTo) {
        String fromToken = from.replace(' ', '_');

        // Remove reference from old target
        removeConnectionReference(from, oldTo);

        // Add reference to new target's equation
        return addConnectionReference(newTo, fromToken);
    }

    /**
     * Adds a reference token to the named element's equation.
     * If the equation is exactly "0", replaces it with the token.
     * Otherwise appends " * token" to the equation.
     */
    private boolean addConnectionReference(String elementName, String token) {
        for (int i = 0; i < flows.size(); i++) {
            FlowDef f = flows.get(i);
            if (f.name().equals(elementName)) {
                String eq = f.equation();
                if (eq.contains(token)) {
                    return true; // Already references this token
                }
                String updated = "0".equals(eq.trim()) ? token : eq + " * " + token;
                flows.set(i, new FlowDef(f.name(), f.comment(), updated,
                        f.timeUnit(), f.source(), f.sink()));
                return true;
            }
        }

        for (int i = 0; i < auxiliaries.size(); i++) {
            AuxDef a = auxiliaries.get(i);
            if (a.name().equals(elementName)) {
                String eq = a.equation();
                if (eq.contains(token)) {
                    return true; // Already references this token
                }
                String updated = "0".equals(eq.trim()) ? token : eq + " * " + token;
                auxiliaries.set(i, new AuxDef(a.name(), a.comment(), updated, a.unit()));
                return true;
            }
        }

        return false; // Target is not a flow or auxiliary
    }

    /**
     * Generates connector routes from the current model state's dependency graph.
     */
    public List<ConnectorRoute> generateConnectors() {
        return ConnectorGenerator.generate(toModelDefinition());
    }
}

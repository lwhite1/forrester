package systems.courant.shrewd.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * One representation of a dynamic system
 */
public class Model extends Element {

    private static final Logger log = LoggerFactory.getLogger(Model.class);

    private final List<Stock> stocks = new ArrayList<>();
    private final List<Flow> flows = new ArrayList<>();
    private final Map<String, Variable> variables = new LinkedHashMap<>();
    private final List<Module> modules = new ArrayList<>();
    private ModelMetadata metadata;

    /**
     * Creates a new model with the given name.
     *
     * @param name the model name
     */
    public Model(String name) {
        super(name);
    }

    /**
     * Returns the attribution and licensing metadata for this model, or {@code null} if none has been set.
     */
    public ModelMetadata getMetadata() {
        return metadata;
    }

    /**
     * Sets attribution and licensing metadata for this model.
     *
     * @param metadata the metadata to associate with this model
     */
    public void setMetadata(ModelMetadata metadata) {
        this.metadata = metadata;
    }

    /**
     * Adds a stock to this model.
     *
     * @throws IllegalArgumentException if a stock with the same name already exists
     */
    public void addStock(Stock stock) {
        for (Stock existing : stocks) {
            if (existing.getName().equals(stock.getName())) {
                throw new IllegalArgumentException(
                        "Duplicate stock name '" + stock.getName() + "' in model '" + getName() + "'");
            }
        }
        stocks.add(stock);
    }

    /**
     * Removes a stock from this model, detaching all connected flows so they do not
     * retain stale references to the removed stock.
     */
    public void removeStock(Stock stock) {
        if (stocks.remove(stock)) {
            for (Flow flow : stock.getInflows()) {
                flow.setSink(null);
            }
            for (Flow flow : stock.getOutflows()) {
                flow.setSource(null);
            }
        }
    }

    /**
     * Adds a variable to this model, keyed by its name.
     *
     * @throws IllegalArgumentException if a variable with the same name already exists
     */
    public void addVariable(Variable variable) {
        Variable existing = variables.get(variable.getName());
        if (existing != null && existing != variable) {
            throw new IllegalArgumentException(
                    "Duplicate variable name '" + variable.getName() + "' in model '" + getName() + "'");
        }
        variables.put(variable.getName(), variable);
    }

    /**
     * Removes a variable from this model.
     */
    public void removeVariable(Variable variable) {
        variables.remove(variable.getName());
    }

    /**
     * Expands an arrayed stock into this model's flat stock list, skipping duplicates.
     */
    public void addArrayedStock(ArrayedStock arrayedStock) {
        for (Stock stock : arrayedStock.getStocks()) {
            if (!stocks.contains(stock)) {
                stocks.add(stock);
            }
        }
    }

    /**
     * Expands an arrayed variable into this model's flat variable map, skipping duplicates.
     */
    public void addArrayedVariable(ArrayedVariable arrayedVariable) {
        for (Variable variable : arrayedVariable.getVariables()) {
            variables.putIfAbsent(variable.getName(), variable);
        }
    }

    /**
     * Expands a multi-arrayed stock into this model's flat stock list, skipping duplicates.
     */
    public void addMultiArrayedStock(MultiArrayedStock multiArrayedStock) {
        for (Stock stock : multiArrayedStock.getStocks()) {
            if (!stocks.contains(stock)) {
                stocks.add(stock);
            }
        }
    }

    /**
     * Expands a multi-arrayed variable into this model's flat variable map, skipping duplicates.
     */
    public void addMultiArrayedVariable(MultiArrayedVariable multiArrayedVariable) {
        for (Variable variable : multiArrayedVariable.getVariables()) {
            variables.putIfAbsent(variable.getName(), variable);
        }
    }

    /**
     * Adds a module to this model, automatically registering its stocks, flows, and
     * variables into the model's own collections. Logs a warning if stock or flow names
     * collide with existing elements. Throws if a variable name collides, since variable
     * lookup is name-keyed and a collision would silently discard the module's variable.
     *
     * @throws IllegalArgumentException if a variable name from the module collides with
     *                                  an existing variable in this model
     */
    public void addModule(Module module) {
        for (Variable variable : module.getVariables()) {
            Variable existing = variables.get(variable.getName());
            if (existing != null && existing != variable) {
                throw new IllegalArgumentException(
                        "Module '" + module.getName() + "' variable '" + variable.getName()
                                + "' collides with existing variable in model '" + getName() + "'");
            }
        }

        modules.add(module);
        for (Stock stock : module.getStocks()) {
            if (!stocks.contains(stock)) {
                boolean nameExists = stocks.stream()
                        .anyMatch(s -> s.getName().equals(stock.getName()));
                if (nameExists) {
                    log.warn("Module '{}' stock '{}' has same name as existing stock in model '{}'",
                            module.getName(), stock.getName(), getName());
                }
                stocks.add(stock);
            }
        }
        for (Flow flow : module.getFlows()) {
            if (!flows.contains(flow)) {
                boolean nameExists = flows.stream()
                        .anyMatch(f -> f.getName().equals(flow.getName()));
                if (nameExists) {
                    log.warn("Module '{}' flow '{}' has same name as existing flow in model '{}'",
                            module.getName(), flow.getName(), getName());
                }
                flows.add(flow);
            }
        }
        for (Variable variable : module.getVariables()) {
            variables.putIfAbsent(variable.getName(), variable);
        }
    }

    /**
     * Adds a module to this model for organization/views without flattening its
     * contents. Unlike {@link #addModule(Module)}, this preserves the module
     * tree hierarchy. Use for compiled models where stocks/flows are already
     * registered at the top level.
     */
    public void addModulePreserved(Module module) {
        modules.add(module);
    }

    /**
     * Returns an unmodifiable list of modules in this model.
     */
    public List<Module> getModules() {
        return Collections.unmodifiableList(modules);
    }

    /**
     * Returns an unmodifiable list of stocks in this model.
     */
    public List<Stock> getStocks() {
        return Collections.unmodifiableList(stocks);
    }

    /**
     * Adds a flow to this model's flow registry.
     *
     * @throws IllegalArgumentException if a flow with the same name already exists
     */
    public void addFlow(Flow flow) {
        for (Flow existing : flows) {
            if (existing.getName().equals(flow.getName())) {
                throw new IllegalArgumentException(
                        "Duplicate flow name '" + flow.getName() + "' in model '" + getName() + "'");
            }
        }
        flows.add(flow);
    }

    /**
     * Returns an unmodifiable list of all flows registered in this model.
     */
    public List<Flow> getFlows() {
        return Collections.unmodifiableList(flows);
    }

    /**
     * Returns an unmodifiable map of variable names to variables.
     */
    public Map<String, Variable> getVariableMap() {
        return Collections.unmodifiableMap(variables);
    }

    /**
     * Returns the names of all stocks in this model.
     */
    public List<String> getStockNames() {
        List<String> results = new ArrayList<>();
        for (Stock stock : stocks) {
            results.add(stock.getName());
        }
        return results;
    }

    /**
     * Returns the current numeric values of all stocks in this model.
     */
    public List<Double> getStockValues() {
        List<Double> results = new ArrayList<>();
        for (Stock stock : stocks) {
            results.add(stock.getQuantity().getValue());
        }
        return results;
    }

    /**
     * Returns the names of all variables in this model.
     */
    public List<String> getVariableNames() {
        return new ArrayList<>(variables.keySet());
    }

    /**
     * Returns an unmodifiable collection of all variables in this model.
     */
    public Collection<Variable> getVariables() {
        return Collections.unmodifiableCollection(variables.values());
    }

    /**
     * Returns the current numeric values of all variables in this model.
     */
    public List<Double> getVariableValues() {
        List<Double> results = new ArrayList<>();
        for (Variable variable : variables.values()) {
            results.add(variable.getValue());
        }
        return results;
    }

    /**
     * Returns the names of all modules in this model.
     */
    public List<String> getModuleNames() {
        List<String> results = new ArrayList<>();
        for (Module module : modules) {
            results.add(module.getName());
        }
        return results;
    }

    /**
     * Returns the variable with the given name, or empty if not found.
     *
     * @param variableName the variable name to look up
     */
    public Optional<Variable> getVariable(String variableName) {
        return Optional.ofNullable(variables.get(variableName));
    }

}

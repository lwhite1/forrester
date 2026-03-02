package com.deathrayresearch.forrester.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One representation of a dynamic system
 */
public class Model extends Element {

    private final List<Stock> stocks = new ArrayList<>();
    private final Map<String, Variable> variables = new LinkedHashMap<>();
    private final List<Module> modules = new ArrayList<>();
    private final List<Constant> constants = new ArrayList<>();

    /**
     * Creates a new model with the given name.
     *
     * @param name the model name
     */
    public Model(String name) {
        super(name);
    }

    /**
     * Adds a stock to this model.
     */
    public void addStock(Stock stock) {
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
     */
    public void addVariable(Variable variable) {
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
     * Adds a module to this model, automatically registering its stocks, variables,
     * and flows into the model's own collections (skipping duplicates).
     */
    public void addModule(Module module) {
        modules.add(module);
        for (Stock stock : module.getStocks()) {
            if (!stocks.contains(stock)) {
                stocks.add(stock);
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
     * Returns the variable with the given name, or {@code null} if not found.
     *
     * @param variableName the variable name to look up
     */
    public Variable getVariable(String variableName) {
        return variables.get(variableName);
    }

    /**
     * Returns an unmodifiable list of constants in this model.
     */
    public List<Constant> getConstants() {
        return Collections.unmodifiableList(constants);
    }

    /**
     * Adds a constant to this model.
     */
    public void addConstant(Constant constant) {
        constants.add(constant);
    }
}

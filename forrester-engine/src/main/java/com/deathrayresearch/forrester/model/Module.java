package com.deathrayresearch.forrester.model;

import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.TimeUnit;

import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Part of a model, broken to reduce complexity in creating and maintaining the model
 */
public class Module extends Element {

    private final Map<String, Stock> stocks = new LinkedHashMap<>();
    private final Map<String, Flow> flows = new LinkedHashMap<>();
    private final Map<String, Variable> variables = new LinkedHashMap<>();
    private final Map<String, Constant> constants = new LinkedHashMap<>();
    private final Map<String, Module> subModules = new LinkedHashMap<>();

    /**
     * Creates a new module with the given name.
     *
     * @param name the module name
     */
    public Module(String name) {
        super(name);
    }

    /**
     * Returns the current quantity of the stock with the given name.
     *
     * @param stockName the name of the stock to look up
     * @throws IllegalArgumentException if no stock with that name exists in this module
     */
    public Quantity valueOfStock(String stockName) {
        Stock stock = stocks.get(stockName);
        Preconditions.checkArgument(stock != null, "No stock found with name: %s", stockName);
        return stock.getQuantity();
    }

    /**
     * Returns the current rate of the flow with the given name, converted to the specified time unit.
     *
     * @param flowName the name of the flow to look up
     * @param timeUnit the time unit to express the rate in
     * @throws IllegalArgumentException if no flow with that name exists in this module
     */
    public Quantity valueOfFlow(String flowName, TimeUnit timeUnit) {
        Flow flow = flows.get(flowName);
        Preconditions.checkArgument(flow != null, "No flow found with name: %s", flowName);
        return flow.flowPerTimeUnit(timeUnit);
    }

    /**
     * Adds a stock to this module.
     */
    public void addStock(Stock stock) {
        stocks.put(stock.getName(), stock);
    }

    /**
     * Expands an arrayed stock into this module's stock map.
     */
    public void addArrayedStock(ArrayedStock arrayedStock) {
        for (Stock stock : arrayedStock.getStocks()) {
            stocks.put(stock.getName(), stock);
        }
    }

    /**
     * Expands a multi-arrayed stock into this module's stock map.
     */
    public void addMultiArrayedStock(MultiArrayedStock multiArrayedStock) {
        for (Stock stock : multiArrayedStock.getStocks()) {
            stocks.put(stock.getName(), stock);
        }
    }

    /**
     * Expands a multi-arrayed variable into this module's variable map.
     */
    public void addMultiArrayedVariable(MultiArrayedVariable multiArrayedVariable) {
        for (Variable variable : multiArrayedVariable.getVariables()) {
            variables.put(variable.getName(), variable);
        }
    }

    /**
     * Adds a flow to this module.
     */
    public void addFlow(Flow rate) {
        flows.put(rate.getName(), rate);
    }

    /**
     * Returns the stock with the given name, or empty if not found.
     */
    public Optional<Stock> getStock(String stockName) {
        return Optional.ofNullable(stocks.get(stockName));
    }

    /**
     * Returns the variable with the given name, or empty if not found.
     */
    public Optional<Variable> getVariable(String variableName) {
        return Optional.ofNullable(variables.get(variableName));
    }

    /**
     * Adds a variable to this module.
     */
    public void addVariable(Variable variable) {
        variables.put(variable.getName(), variable);
    }

    /**
     * Returns an unmodifiable collection of all variables in this module.
     */
    public Collection<Variable> getVariables() {
        return Collections.unmodifiableCollection(variables.values());
    }

    /**
     * Returns an unmodifiable list of all stocks in this module.
     */
    public List<Stock> getStocks() {
        return Collections.unmodifiableList(new ArrayList<>(stocks.values()));
    }

    /**
     * Expands an arrayed variable into this module's variable map.
     */
    public void addArrayedVariable(ArrayedVariable arrayedVariable) {
        for (Variable variable : arrayedVariable.getVariables()) {
            variables.put(variable.getName(), variable);
        }
    }

    /**
     * Expands an arrayed flow into this module's flow map.
     */
    public void addArrayedFlow(ArrayedFlow arrayedFlow) {
        for (Flow flow : arrayedFlow.getFlows()) {
            flows.put(flow.getName(), flow);
        }
    }

    /**
     * Returns an unmodifiable collection of all flows in this module.
     */
    public Collection<Flow> getFlows() {
        return Collections.unmodifiableCollection(flows.values());
    }

    /**
     * Adds a constant to this module.
     */
    public void addConstant(Constant constant) {
        constants.put(constant.getName(), constant);
    }

    /**
     * Returns the constant with the given name, or empty if not found.
     */
    public Optional<Constant> getConstant(String name) {
        return Optional.ofNullable(constants.get(name));
    }

    /**
     * Returns an unmodifiable map of constants in this module.
     */
    public Map<String, Constant> getConstants() {
        return Collections.unmodifiableMap(constants);
    }

    /**
     * Adds a sub-module to this module.
     *
     * @throws IllegalArgumentException if the module is this module (self-reference)
     */
    public void addSubModule(Module module) {
        if (module == this) {
            throw new IllegalArgumentException(
                    "Cannot add module '" + module.getName() + "' as its own sub-module");
        }
        subModules.put(module.getName(), module);
    }

    /**
     * Returns the sub-module with the given name, or empty if not found.
     */
    public Optional<Module> getSubModule(String name) {
        return Optional.ofNullable(subModules.get(name));
    }

    /**
     * Returns an unmodifiable map of sub-modules in this module.
     */
    public Map<String, Module> getSubModules() {
        return Collections.unmodifiableMap(subModules);
    }
}

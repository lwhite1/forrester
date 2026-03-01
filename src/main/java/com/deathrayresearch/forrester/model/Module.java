package com.deathrayresearch.forrester.model;

import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.TimeUnit;

import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Part of a model, broken to reduce complexity in creating and maintaining the model
 */
public class Module extends Element {

    private final Map<String, Stock> stocks = new HashMap<>();
    private final Map<String, Flow> flows = new HashMap<>();
    private final Map<String, Variable> variables = new HashMap<>();

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
     * Adds a flow to this module.
     */
    public void addFlow(Flow rate) {
        flows.put(rate.getName(), rate);
    }

    /**
     * Returns the stock with the given name, or {@code null} if not found.
     */
    public Stock getStock(String stockName) {
        return stocks.get(stockName);
    }

    /**
     * Returns the variable with the given name, or {@code null} if not found.
     */
    public Variable getVariable(String variableName) {
        return variables.get(variableName);
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
     * Returns all stocks in this module.
     */
    public List<Stock> getStocks() {
        return new ArrayList<>(stocks.values());
    }
}

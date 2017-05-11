package com.deathrayresearch.dynamics.model;

import com.deathrayresearch.dynamics.measure.Quantity;
import com.deathrayresearch.dynamics.measure.Unit;
import com.deathrayresearch.dynamics.measure.dimension.Time;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class SubSystem extends Element {

    private Map<String, Stock> stocks = new HashMap<>();
    private Map<String, Flow> flows = new HashMap<>();
    private Map<String, Variable> variables = new HashMap<>();

    public SubSystem(String name) {
        super(name);
    }

    public Quantity valueOfStock(String stockName) {
        return stocks.get(stockName).getCurrentValue();
    }

    public Quantity valueOfFlow(String flowName, Unit<Time> timeUnit) {
        return flows.get(flowName).getRate().flowPerTimeUnit(timeUnit);
    }

    public void addStock(Stock stock) {
        stocks.put(stock.getName(), stock);
    }

    public void addFlow(Flow rate) {
        flows.put(rate.getName(), rate);
    }

    public Stock getStock(String stockName) {
        return stocks.get(stockName);
    }

    public Variable getVariable(String variableName) {
        return variables.get(variableName);
    }

    public void addVariable(Variable variable) {
        variables.put(variable.getName(), variable);
    }
}

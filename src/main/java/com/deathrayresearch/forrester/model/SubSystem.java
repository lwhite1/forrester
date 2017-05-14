package com.deathrayresearch.forrester.model;

import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.TimeUnit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Part of a model, broken to reduce complexity in creating and maintaining the model
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

    public Quantity valueOfFlow(String flowName, TimeUnit timeUnit) {
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

    public List<Stock> getStocks() {
        List<Stock> stockList = new ArrayList<>();
        stockList.addAll(stocks.values());
        return stockList;
    }
}

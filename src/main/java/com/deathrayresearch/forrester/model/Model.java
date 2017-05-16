package com.deathrayresearch.forrester.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * One representation of a dynamic system
 */
public class Model {

    private String name;
    private List<Stock> stocks = new ArrayList<>();
    private Map<String, Variable> variables = new HashMap<>();
    private List<Module> modules = new ArrayList<>();

    public Model(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void addStock(Stock stock) {
        stocks.add(stock);
    }

    public void removeStock(Stock stock) {
        stocks.remove(stock);
    }

    public void addVariable(Variable variable) {
        variables.put(variable.getName(), variable);
    }

    public void removeVariable(Variable variable) {
        variables.remove(variable.getName());
    }

    public void addSubSystem(Module module) {
        modules.add(module);
    }

    public List<Module> getModules() {
        return modules;
    }

    public List<Stock> getStocks() {
        return stocks;
    }

    public Map<String, Variable> getVariables() {
        return variables;
    }

    public List<String> getStockNames() {
        List<String> results = new ArrayList<>();
        for (Stock stock : stocks) {
            results.add(stock.getName());
        }
        return results;
    }
    public List<Double> getStockValues() {
        List<Double> results = new ArrayList<>();
        for (Stock stock : stocks) {
            results.add(stock.getCurrentValue().getValue());
        }
        return results;
    }

    public List<String> getVariableNames() {
        List<String> results = new ArrayList<>();
        results.addAll(variables.keySet());
        return results;
    }

    public List<Double> getVariableValues() {
        List<Double> results = new ArrayList<>();
        for (Variable variable : variables.values()) {
            results.add(variable.getCurrentValue());
        }
        return results;
    }

    public List<String> getSubSystemNames() {
        List<String> results = new ArrayList<>();
        for (Module module : modules) {
            results.add(module.getName());
        }
        return results;
    }

    public Variable getVariable(String variableName) {
        return variables.get(variableName);
    }
}

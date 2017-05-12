package com.deathrayresearch.dynamics.model;

import com.deathrayresearch.dynamics.measure.Dimension;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class Model {

    private String name;
    private List<Stock<Dimension>> stocks = new ArrayList<>();
    private List<Variable> variables = new ArrayList<>();
    private List<SubSystem> subSystems = new ArrayList<>();

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
        variables.add(variable);
    }

    public void removeVariable(Variable variable) {
        variables.remove(variable);
    }

    public void addSubSystem(SubSystem subSystem) {
        subSystems.add(subSystem);
    }

    public List<SubSystem> getSubSystems() {
        return subSystems;
    }

    public List<Stock<Dimension>> getStocks() {
        return stocks;
    }

    public List<Variable> getVariables() {
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
        for (Variable variable : variables) {
            results.add(variable.getName());
        }
        return results;
    }

    public List<Double> getVariableValues() {
        List<Double> results = new ArrayList<>();
        for (Variable variable : variables) {
            results.add(variable.getCurrentValue());
        }
        return results;
    }

    public List<String> getSubSystemNames() {
        List<String> results = new ArrayList<>();
        for (SubSystem subSystem : subSystems) {
            results.add(subSystem.getName());
        }
        return results;
    }
}

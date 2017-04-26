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

    public List<Stock<Dimension>> getStocks() {
        return stocks;
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
}

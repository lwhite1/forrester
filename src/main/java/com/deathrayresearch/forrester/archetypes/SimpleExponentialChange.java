package com.deathrayresearch.forrester.archetypes;

import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.model.Stock;

/**
 * Encapsulates a formula for exponential growth or decay. Growth if used in an input flow, decay if used in an output
 * flow
 */
public class SimpleExponentialChange {

    private final double growthAmount;
    private final Stock stock;
    private final String name;

    public static Quantity from(String name, Stock stock, double growthAmount) {
        return new SimpleExponentialChange(name, stock, growthAmount).getCurrentQuantity();
    }

    private SimpleExponentialChange(String name, Stock stock, double growthAmount) {
        this.name = name;
        this.growthAmount = growthAmount;
        this.stock = stock;
    }

    public Quantity getCurrentQuantity() {
        return new Quantity(name,stock.getCurrentValue().getValue() * growthAmount, stock.getUnit());
    }
}

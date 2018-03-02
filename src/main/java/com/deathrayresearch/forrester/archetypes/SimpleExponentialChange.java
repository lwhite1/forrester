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

    public static Quantity from(Stock stock, double growthAmount) {
        return new SimpleExponentialChange(stock, growthAmount).getCurrentQuantity();
    }

    private SimpleExponentialChange(Stock stock, double growthAmount) {
        this.growthAmount = growthAmount;
        this.stock = stock;
    }

    public Quantity getCurrentQuantity() {
        return new Quantity(stock.getQuantity().getValue() * growthAmount, stock.getUnit());
    }
}

package com.deathrayresearch.forrester.archetypes;

import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.model.Stock;

/**
 * Encapsulates a formula for linear growth or decay. Growth if used in an input flow, decay if used in an output
 * flow.  For each time step, a constant amount is added.
 */
public class SimpleLinearChange {

    private final double growthAmount;
    private final Stock stock;

    public static Quantity from(Stock stock, double growthAmount) {
        return new SimpleLinearChange(stock, growthAmount).getCurrentQuantity();
    }

    private SimpleLinearChange(Stock stock, double growthAmount) {
        this.growthAmount = growthAmount;
        this.stock = stock;
    }

    public Quantity getCurrentQuantity() {
        return new Quantity(growthAmount, stock.getUnit());
    }
}

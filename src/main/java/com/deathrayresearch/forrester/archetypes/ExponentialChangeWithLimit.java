package com.deathrayresearch.forrester.archetypes;

import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.model.Stock;

/**
 * Encapsulates a formula for exponential growth or decay. Growth if used in an input flow, decay if used in an output
 * flow. The growth is limited and so declines as it approaches its limit, creating an s-shaped curve. Adoption in a
 * market of a fixed size is one example application. Adoption starts slow, increases rapidly as awareness spreads,
 * then slows again as it approaches the limit of the size of the addressable market.
 */
public class ExponentialChangeWithLimit {

    private final double growthAmount;
    private final Stock stock;
    private final String name;
    private final double limit;

    public static Quantity from(String name, Stock stock, double growthAmount, double limit) {
        return new ExponentialChangeWithLimit(name, stock, growthAmount, limit).getCurrentQuantity();
    }

    private ExponentialChangeWithLimit(String name, Stock stock, double growthAmount, double limit) {
        this.name = name;
        this.growthAmount = growthAmount;
        this.stock = stock;
        this.limit = limit;
    }

    public Quantity getCurrentQuantity() {
        double ratio = stock.getCurrentValue().getValue() / limit;
        double result = stock.getCurrentValue().getValue() * growthAmount * (1 - ratio);
        return new Quantity(name, result, stock.getUnit());
    }
}

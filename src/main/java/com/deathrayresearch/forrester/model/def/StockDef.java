package com.deathrayresearch.forrester.model.def;

/**
 * Definition of a stock (level/accumulator) in a model.
 *
 * @param name the stock name
 * @param comment optional description
 * @param initialValue the initial numeric value
 * @param unit the unit name (resolved at compile time)
 * @param negativeValuePolicy optional policy name ("CLAMP_TO_ZERO", "ALLOW", "THROW"), or null for default
 */
public record StockDef(
        String name,
        String comment,
        double initialValue,
        String unit,
        String negativeValuePolicy
) {

    public StockDef {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Stock name must not be blank");
        }
    }

    public StockDef(String name, double initialValue, String unit) {
        this(name, null, initialValue, unit, null);
    }
}

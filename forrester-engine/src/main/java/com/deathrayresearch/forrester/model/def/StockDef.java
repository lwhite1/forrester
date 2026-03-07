package com.deathrayresearch.forrester.model.def;

import java.util.List;

/**
 * Definition of a stock (level/accumulator) in a model.
 *
 * @param name the stock name
 * @param comment optional description
 * @param initialValue the initial numeric value
 * @param unit the unit name (resolved at compile time)
 * @param negativeValuePolicy optional policy name ("CLAMP_TO_ZERO", "ALLOW", "THROW"), or null for default
 * @param subscripts dimension names this stock is subscripted over (empty for scalar)
 */
public record StockDef(
        String name,
        String comment,
        double initialValue,
        String unit,
        String negativeValuePolicy,
        List<String> subscripts
) implements ElementDef {

    public StockDef {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Stock name must not be blank");
        }
        if (Double.isNaN(initialValue) || Double.isInfinite(initialValue)) {
            throw new IllegalArgumentException(
                    "Stock '" + name + "' initialValue must be finite, got " + initialValue);
        }
        subscripts = subscripts == null ? List.of() : List.copyOf(subscripts);
    }

    /**
     * Backward-compatible constructor without subscripts.
     */
    public StockDef(String name, String comment, double initialValue, String unit,
                    String negativeValuePolicy) {
        this(name, comment, initialValue, unit, negativeValuePolicy, List.of());
    }

    /**
     * Convenience constructor that creates a stock with no comment and the default negative value policy.
     *
     * @param name         the stock name
     * @param initialValue the initial numeric value
     * @param unit         the unit name
     */
    public StockDef(String name, double initialValue, String unit) {
        this(name, null, initialValue, unit, null, List.of());
    }
}

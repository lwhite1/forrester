package com.deathrayresearch.forrester.model.def;

/**
 * Definition of a constant (fixed exogenous value) in a model.
 *
 * @param name the constant name
 * @param comment optional description
 * @param value the numeric value
 * @param unit the unit name
 */
public record ConstantDef(
        String name,
        String comment,
        double value,
        String unit
) {

    public ConstantDef {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Constant name must not be blank");
        }
    }

    public ConstantDef(String name, double value, String unit) {
        this(name, null, value, unit);
    }
}

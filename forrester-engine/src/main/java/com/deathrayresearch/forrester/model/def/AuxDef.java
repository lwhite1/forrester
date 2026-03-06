package com.deathrayresearch.forrester.model.def;

/**
 * Definition of an auxiliary variable (calculated intermediate value) in a model.
 *
 * @param name the variable name
 * @param comment optional description
 * @param equation the formula expression string
 * @param unit the unit name
 */
public record AuxDef(
        String name,
        String comment,
        String equation,
        String unit
) implements ElementDef {

    public AuxDef {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Auxiliary name must not be blank");
        }
        if (equation == null || equation.isBlank()) {
            throw new IllegalArgumentException("Auxiliary equation must not be blank");
        }
    }

    public AuxDef(String name, String equation, String unit) {
        this(name, null, equation, unit);
    }
}

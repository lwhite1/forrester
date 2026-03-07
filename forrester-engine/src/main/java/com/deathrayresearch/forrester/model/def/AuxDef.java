package com.deathrayresearch.forrester.model.def;

import java.util.List;

/**
 * Definition of an auxiliary variable (calculated intermediate value) in a model.
 *
 * @param name the variable name
 * @param comment optional description
 * @param equation the formula expression string
 * @param unit the unit name
 * @param subscripts dimension names this auxiliary is subscripted over (empty for scalar)
 */
public record AuxDef(
        String name,
        String comment,
        String equation,
        String unit,
        List<String> subscripts
) implements ElementDef {

    public AuxDef {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Auxiliary name must not be blank");
        }
        if (equation == null || equation.isBlank()) {
            throw new IllegalArgumentException("Auxiliary equation must not be blank");
        }
        subscripts = subscripts == null ? List.of() : List.copyOf(subscripts);
    }

    /**
     * Backward-compatible constructor without subscripts.
     */
    public AuxDef(String name, String comment, String equation, String unit) {
        this(name, comment, equation, unit, List.of());
    }

    /**
     * Convenience constructor that creates an auxiliary definition without a comment.
     */
    public AuxDef(String name, String equation, String unit) {
        this(name, null, equation, unit, List.of());
    }
}

package com.deathrayresearch.forrester.model.def;

/**
 * A qualitative variable in a causal loop diagram. Unlike stocks, flows, and auxiliaries,
 * CLD variables have no equation or unit — they represent causal concepts that have not
 * yet been formalized into stock-and-flow elements.
 *
 * @param name the variable name
 * @param comment optional description
 */
public record CldVariableDef(
        String name,
        String comment
) implements ElementDef {

    public CldVariableDef {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("CLD variable name must not be blank");
        }
    }

    /**
     * Creates a CLD variable definition without a comment.
     *
     * @param name the variable name
     */
    public CldVariableDef(String name) {
        this(name, null);
    }
}

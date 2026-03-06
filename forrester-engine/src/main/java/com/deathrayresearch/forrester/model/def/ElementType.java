package com.deathrayresearch.forrester.model.def;

import java.util.Locale;

/**
 * Type of element in a model definition view.
 * Each value maps to a lowercase label used in JSON/XMILE serialization.
 */
public enum ElementType {

    STOCK("stock"),
    FLOW("flow"),
    AUX("aux"),
    CONSTANT("constant"),
    MODULE("module"),
    LOOKUP("lookup"),
    CLD_VARIABLE("cld_variable");

    private final String label;

    ElementType(String label) {
        this.label = label;
    }

    /**
     * Returns the lowercase label for serialization (e.g. "stock", "flow").
     */
    public String label() {
        return label;
    }

    /**
     * Parses a label string (case-insensitive) into an ElementType.
     *
     * @throws IllegalArgumentException if the label does not match any type
     */
    public static ElementType fromLabel(String label) {
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("Element type label must not be blank");
        }
        String normalized = label.toLowerCase(Locale.ROOT);
        for (ElementType type : values()) {
            if (type.label.equals(normalized)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown element type: '" + label + "'");
    }
}

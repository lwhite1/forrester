package com.deathrayresearch.forrester.model.compile;

import java.util.Arrays;
import java.util.List;

/**
 * Utility for parsing and working with dot-separated qualified names
 * used in nested module hierarchies (e.g. "Workforce.Total Workforce").
 */
public record QualifiedName(List<String> parts) {

    public QualifiedName {
        parts = List.copyOf(parts);
        if (parts.isEmpty()) {
            throw new IllegalArgumentException("Qualified name must have at least one part");
        }
    }

    /**
     * Parses a dot-separated qualified name string.
     */
    public static QualifiedName parse(String name) {
        return new QualifiedName(Arrays.asList(name.split("\\.")));
    }

    /**
     * Returns the leaf (last) part of the qualified name.
     */
    public String leaf() {
        return parts.get(parts.size() - 1);
    }

    /**
     * Returns true if the name has more than one part (is qualified).
     */
    public boolean isQualified() {
        return parts.size() > 1;
    }

    @Override
    public String toString() {
        return String.join(".", parts);
    }
}

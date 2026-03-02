package com.deathrayresearch.forrester.model.def;

import java.util.Locale;
import java.util.Set;

/**
 * Placement of an element in a graphical view.
 *
 * @param name the element name (must match a stock, flow, aux, constant, module, or lookup)
 * @param type the element type ("stock", "flow", "aux", "constant", "module", "lookup")
 * @param x the x-coordinate
 * @param y the y-coordinate
 */
public record ElementPlacement(
        String name,
        String type,
        double x,
        double y
) {

    private static final Set<String> VALID_TYPES = Set.of(
            "stock", "flow", "aux", "constant", "module", "lookup");

    public ElementPlacement {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Element name must not be blank");
        }
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("Element type must not be blank");
        }
        if (!VALID_TYPES.contains(type.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException(
                    "Element type must be one of " + VALID_TYPES + ", got '" + type + "'");
        }
    }
}

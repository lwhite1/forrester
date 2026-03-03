package com.deathrayresearch.forrester.model.def;

/**
 * Placement of an element in a graphical view.
 *
 * @param name the element name (must match a stock, flow, aux, constant, module, or lookup)
 * @param type the element type
 * @param x the x-coordinate
 * @param y the y-coordinate
 */
public record ElementPlacement(
        String name,
        ElementType type,
        double x,
        double y
) {

    public ElementPlacement {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Element name must not be blank");
        }
        if (type == null) {
            throw new IllegalArgumentException("Element type must not be null");
        }
    }
}

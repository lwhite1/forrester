package com.deathrayresearch.forrester.model.def;

/**
 * Placement of an element in a graphical view.
 *
 * @param name the element name (must match a stock, flow, aux, constant, module, or lookup)
 * @param type the element type
 * @param x the x-coordinate
 * @param y the y-coordinate
 * @param width custom width (0 = use default for the element type)
 * @param height custom height (0 = use default for the element type)
 */
public record ElementPlacement(
        String name,
        ElementType type,
        double x,
        double y,
        double width,
        double height
) {

    /**
     * Backward-compatible constructor that uses default (0) width and height.
     */
    public ElementPlacement(String name, ElementType type, double x, double y) {
        this(name, type, x, y, 0, 0);
    }

    public ElementPlacement {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Element name must not be blank");
        }
        if (type == null) {
            throw new IllegalArgumentException("Element type must not be null");
        }
    }

    /**
     * Returns true if this placement has a custom (non-default) size.
     */
    public boolean hasCustomSize() {
        return width > 0 && height > 0;
    }
}

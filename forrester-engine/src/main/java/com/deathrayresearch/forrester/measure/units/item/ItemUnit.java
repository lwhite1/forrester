package com.deathrayresearch.forrester.measure.units.item;

import com.deathrayresearch.forrester.measure.Dimension;
import com.deathrayresearch.forrester.measure.Unit;

/**
 * A custom item unit with a user-defined name, for domain-specific countable quantities
 * beyond the built-in {@link ItemUnits} (PEOPLE, THING, etc.).
 *
 * <p>Use this class whenever your model needs a unit that doesn't exist in {@link ItemUnits}.
 * Each distinct name creates a logically separate unit. Two {@code ItemUnit} instances with
 * the same name are considered equal.
 *
 * <pre>{@code
 * // Define domain-specific units
 * Unit WIDGETS = new ItemUnit("Widget");
 * Unit ERRORS = new ItemUnit("Error");
 * Unit DOLLARS = new ItemUnit("Dollar");
 *
 * // Use them with stocks, flows, and quantities
 * Stock inventory = new Stock("Inventory", 100.0, WIDGETS);
 * Flow production = Flow.create("Production", TimeUnits.DAY,
 *         () -> new Quantity(10, WIDGETS));
 * }</pre>
 *
 * <p>All ItemUnit instances belong to {@link com.deathrayresearch.forrester.measure.Dimension#ITEM}
 * and have a base-unit ratio of 1.0 (there is no conversion hierarchy among item units).
 *
 * @see ItemUnits
 */
public class ItemUnit implements Unit {

    private final String name;

    /**
     * Creates a new item unit with the given name.
     *
     * @param name the display name for this unit (e.g., "Widget", "Patient")
     */
    public ItemUnit(String name) {
        this.name = name;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return name;
    }

    /** {@inheritDoc} Returns {@link Dimension#ITEM}. */
    @Override
    public Dimension getDimension() {
        return Dimension.ITEM;
    }

    /** {@inheritDoc} Always returns {@code 1.0} since item units have no conversion hierarchy. */
    @Override
    public double ratioToBaseUnit() {
        return 1.0;
    }

    /**
     * Two {@code ItemUnit} instances are equal if they have the same name.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ItemUnit itemUnit = (ItemUnit) o;
        return name.equals(itemUnit.name);
    }

    /** Returns a hash code based on this unit's name. */
    @Override
    public int hashCode() {
        return name.hashCode();
    }

    /** Returns the unit name. */
    @Override
    public String toString() {
        return name;
    }
}

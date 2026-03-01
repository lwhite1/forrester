package com.deathrayresearch.forrester.measure.units.item;

import com.deathrayresearch.forrester.measure.Dimension;
import com.deathrayresearch.forrester.measure.Unit;

/**
 * A custom item unit with a user-defined name. Allows models to use domain-specific
 * countable units (e.g., "widgets", "patients") beyond the built-in {@link ItemUnits}.
 */
public class ItemUnit implements Unit {

    private final String name;

    public ItemUnit(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Dimension getDimension() {
        return Dimension.ITEM;
    }

    @Override
    public double ratioToBaseUnit() {
        return 1.0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ItemUnit itemUnit = (ItemUnit) o;
        return name.equals(itemUnit.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }
}

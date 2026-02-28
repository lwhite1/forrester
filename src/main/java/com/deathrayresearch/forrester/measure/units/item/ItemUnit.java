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
}

package com.deathrayresearch.forrester.measure.units.item;

import com.deathrayresearch.forrester.measure.Dimension;
import com.deathrayresearch.forrester.measure.Unit;

/**
 * Built-in units for countable items such as generic things and people.
 */
public enum ItemUnits implements Unit {

    THING("Thing", 1.0),
    PEOPLE("Person", 1.0);

    private final String name;
    private final double ratioToBaseUnit;

    ItemUnits(String name, double ratioToBaseUnit) {
        this.name = name;
        this.ratioToBaseUnit = ratioToBaseUnit;
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
        return ratioToBaseUnit;
    }
}

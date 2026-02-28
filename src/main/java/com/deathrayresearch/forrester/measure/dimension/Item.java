package com.deathrayresearch.forrester.measure.dimension;

import com.deathrayresearch.forrester.measure.Dimension;
import com.deathrayresearch.forrester.measure.Unit;
import com.deathrayresearch.forrester.measure.units.item.ItemUnits;

/**
 * The item (countable entity) dimension. Base unit is "thing".
 */
public enum Item implements Dimension {

    INSTANCE;

    @Override
    public Unit getBaseUnit() {
        return ItemUnits.THING;
    }
}

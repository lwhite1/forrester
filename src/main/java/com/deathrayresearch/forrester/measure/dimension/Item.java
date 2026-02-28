package com.deathrayresearch.forrester.measure.dimension;

import com.deathrayresearch.forrester.measure.Dimension;
import com.deathrayresearch.forrester.measure.Unit;
import com.deathrayresearch.forrester.measure.units.item.ItemUnits;

public enum Item implements Dimension {

    INSTANCE;

    public static Item getInstance() {
        return INSTANCE;
    }

    @Override
    public Unit getBaseUnit() {
        return ItemUnits.THING;
    }
}

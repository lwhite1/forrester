package com.deathrayresearch.forrester.measure.units.item;

import com.deathrayresearch.forrester.measure.Dimension;
import com.deathrayresearch.forrester.measure.Unit;
import com.deathrayresearch.forrester.measure.dimension.Item;

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
        return Item.INSTANCE;
    }

    @Override
    public double ratioToBaseUnit() {
        return 1.0;
    }
}

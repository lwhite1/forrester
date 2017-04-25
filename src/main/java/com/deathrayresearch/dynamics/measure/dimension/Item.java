package com.deathrayresearch.dynamics.measure.dimension;

import com.deathrayresearch.dynamics.measure.Converter;
import com.deathrayresearch.dynamics.measure.Dimension;
import com.deathrayresearch.dynamics.measure.Unit;
import com.deathrayresearch.dynamics.measure.units.item.One;

/**
 *
 */
public class Item implements Dimension {

    private static Item ourInstance = new Item();

    public static Item getInstance() {
        return ourInstance;
    }

    private Item() {}

    @Override
    public Unit getBaseUnit() {
        return One.getInstance();
    }

    @Override
    public Converter getConverter() {
        return null;
    }
}

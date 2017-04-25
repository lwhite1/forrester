package com.deathrayresearch.dynamics.measure.units.item;

import com.deathrayresearch.dynamics.measure.Dimension;
import com.deathrayresearch.dynamics.measure.Unit;
import com.deathrayresearch.dynamics.measure.dimension.Item;
import com.deathrayresearch.dynamics.measure.dimension.Time;

/**
 * The base unit for items
 */
public class One implements Unit<Item> {

    public static final String NAME = "One";
    public static final Dimension DIMENSION = Item.getInstance();
    private static final One instance = new One();

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Dimension getDimension() {
        return DIMENSION;
    }

    @Override
    public double ratioToBaseUnit() {
        return 1.0;
    }

    public static One getInstance() {
        return instance;
    }


}

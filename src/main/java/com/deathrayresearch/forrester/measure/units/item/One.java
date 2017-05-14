package com.deathrayresearch.forrester.measure.units.item;

import com.deathrayresearch.forrester.measure.Dimension;
import com.deathrayresearch.forrester.measure.Unit;
import com.deathrayresearch.forrester.measure.dimension.Item;

/**
 * The base unit for items
 */
public class One implements Unit {

    public static final String NAME = "One";
    public static final Dimension DIMENSION = Dimension.ITEM;
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

package com.deathrayresearch.forrester.measure.units.item;

import com.deathrayresearch.forrester.measure.Dimension;
import com.deathrayresearch.forrester.measure.Unit;

/**
 * The base unit for items. Subclass as needed
 */
public class Thing implements Unit {

    public static final String NAME = "Thing";
    public static final Dimension DIMENSION = Dimension.ITEM;
    private static final Thing instance = new Thing();

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

    public static Thing getInstance() {
        return instance;
    }


}

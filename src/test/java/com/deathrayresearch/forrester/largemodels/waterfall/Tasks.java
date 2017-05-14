package com.deathrayresearch.forrester.largemodels.waterfall;


import com.deathrayresearch.forrester.measure.Dimension;
import com.deathrayresearch.forrester.measure.Unit;
import com.deathrayresearch.forrester.measure.dimension.Item;

/**
 *
 */

class Tasks implements Unit {

    private static final Tasks instance = new Tasks();

    @Override
    public String getName() {
        return "Task";
    }

    @Override
    public Dimension getDimension() {
        return Item.getInstance();
    }

    @Override
    public double ratioToBaseUnit() {
        return 1.0;
    }

    static Tasks getInstance() {
        return instance;
    }
}

package com.deathrayresearch.dynamics.largemodels.waterfall;

import com.deathrayresearch.dynamics.measure.Dimension;
import com.deathrayresearch.dynamics.measure.Unit;
import com.deathrayresearch.dynamics.measure.dimension.Item;

/**
 *
 */

class Tasks implements Unit<Item> {

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

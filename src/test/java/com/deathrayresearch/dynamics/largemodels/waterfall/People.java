package com.deathrayresearch.dynamics.largemodels.waterfall;

import com.deathrayresearch.dynamics.measure.Dimension;
import com.deathrayresearch.dynamics.measure.Unit;
import com.deathrayresearch.dynamics.measure.dimension.Item;

/**
 *
 */

class People implements Unit<Item> {

    private static final People instance = new People();

    @Override
    public String getName() {
        return "Person";
    }

    @Override
    public Dimension getDimension() {
        return Item.getInstance();
    }

    @Override
    public double ratioToBaseUnit() {
        return 1.0;
    }

    static People getInstance() {
        return instance;
    }
}

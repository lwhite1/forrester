package com.deathrayresearch.forrester.largemodels.waterfall;


import com.deathrayresearch.forrester.measure.Dimension;
import com.deathrayresearch.forrester.measure.Unit;
import com.deathrayresearch.forrester.measure.dimension.Item;

/**
 *
 */

class People implements Unit {

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

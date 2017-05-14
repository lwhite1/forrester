package com.deathrayresearch.forrester.largemodels.waterfall;


import com.deathrayresearch.forrester.measure.Dimension;
import com.deathrayresearch.forrester.measure.Unit;
import com.deathrayresearch.forrester.measure.dimension.Item;

/**
 *
 */

class PersonDays implements Unit {

    private static final PersonDays instance = new PersonDays();

    @Override
    public String getName() {
        return "Person day";
    }

    @Override
    public Dimension getDimension() {
        return Item.getInstance();
    }

    @Override
    public double ratioToBaseUnit() {
        return 1.0;
    }

    static PersonDays getInstance() {
        return instance;
    }
}

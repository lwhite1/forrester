package com.deathrayresearch.forrester.largemodels.waterfall.units;


import com.deathrayresearch.forrester.measure.Dimension;
import com.deathrayresearch.forrester.measure.Unit;
import com.deathrayresearch.forrester.measure.dimension.Item;

/**
 *
 */

public class PersonDays implements Unit {

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

    public static PersonDays getInstance() {
        return instance;
    }
}

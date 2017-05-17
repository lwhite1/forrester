package com.deathrayresearch.forrester.largemodels.waterfall.units;


import com.deathrayresearch.forrester.measure.Dimension;
import com.deathrayresearch.forrester.measure.Unit;
import com.deathrayresearch.forrester.measure.dimension.Item;

/**
 *
 */

public class PersonDaysPerDay implements Unit {

    private static final PersonDaysPerDay instance = new PersonDaysPerDay();

    @Override
    public String getName() {
        return "Person days per day";
    }

    @Override
    public Dimension getDimension() {
        return Item.getInstance();
    }

    @Override
    public double ratioToBaseUnit() {
        return 1.0;
    }

    public static PersonDaysPerDay getInstance() {
        return instance;
    }
}

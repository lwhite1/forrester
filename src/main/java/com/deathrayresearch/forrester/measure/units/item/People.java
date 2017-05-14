package com.deathrayresearch.forrester.measure.units.item;


import com.deathrayresearch.forrester.measure.Dimension;
import com.deathrayresearch.forrester.measure.Unit;
import com.deathrayresearch.forrester.measure.dimension.Item;

/**
 *
 */
public class People extends Thing {

    private static final People instance = new People();

    @Override
    public String getName() {
        return "Person";
    }

    @Override
    public Dimension getDimension() {
        return Dimension.ITEM;
    }

    @Override
    public double ratioToBaseUnit() {
        return 1.0;
    }

    public static People getInstance() {
        return instance;
    }
}

package com.deathrayresearch.forrester.measure.units.length;


import com.deathrayresearch.forrester.measure.Dimension;
import com.deathrayresearch.forrester.measure.Unit;
import com.deathrayresearch.forrester.measure.dimension.Length;

/**
 *
 */
public class Mile implements Unit {

    public static final String NAME = "Mile";
    public static final Dimension DIMENSION = Length.getInstance();
    private static final Mile instance = new Mile();

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
        return 1609.34;
    }
    public static Mile getInstance() {
        return instance;
    }
}

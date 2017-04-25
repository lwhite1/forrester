package com.deathrayresearch.dynamics.measure.units.length;

import com.deathrayresearch.dynamics.measure.Dimension;
import com.deathrayresearch.dynamics.measure.Unit;
import com.deathrayresearch.dynamics.measure.dimension.Length;

/**
 *
 */
public class Mile implements Unit<Length> {

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

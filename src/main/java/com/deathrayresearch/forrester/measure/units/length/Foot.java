package com.deathrayresearch.forrester.measure.units.length;


import com.deathrayresearch.forrester.measure.Dimension;
import com.deathrayresearch.forrester.measure.Unit;
import com.deathrayresearch.forrester.measure.dimension.Length;

/**
 *
 */
public class Foot implements Unit {

    public static final String NAME = "Foot";
    public static final Dimension DIMENSION = Length.getInstance();
    private static final Foot instance = new Foot();

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
        return 0.3047992424196;
    }
    public static Foot getInstance() {
        return instance;
    }
}

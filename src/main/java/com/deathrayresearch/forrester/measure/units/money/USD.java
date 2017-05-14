package com.deathrayresearch.forrester.measure.units.money;


import com.deathrayresearch.forrester.measure.Dimension;
import com.deathrayresearch.forrester.measure.Unit;
import com.deathrayresearch.forrester.measure.dimension.Time;

/**
 *
 */
public class USD implements Unit {

    public static final String NAME = "USD";
    public static final Dimension DIMENSION = Time.getInstance();
    private static final USD instance = new USD();

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
        return 1.0;
    }

    public static USD getInstance() {
        return instance;
    }
}

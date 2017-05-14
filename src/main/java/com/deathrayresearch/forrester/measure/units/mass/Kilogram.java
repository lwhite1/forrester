package com.deathrayresearch.forrester.measure.units.mass;

import com.deathrayresearch.forrester.measure.Dimension;
import com.deathrayresearch.forrester.measure.Unit;
import com.deathrayresearch.forrester.measure.dimension.Mass;

/**
 *
 */
public class Kilogram implements Unit {

    public static final String NAME = "Kilogram";
    public static final Dimension DIMENSION = Mass.getInstance();
    private static final Kilogram instance = new Kilogram();

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

    public static Kilogram getInstance() {
        return instance;
    }

}

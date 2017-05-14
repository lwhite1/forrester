package com.deathrayresearch.forrester.measure.units.mass;

import com.deathrayresearch.forrester.measure.Dimension;
import com.deathrayresearch.forrester.measure.Unit;
import com.deathrayresearch.forrester.measure.dimension.Mass;

/**
 *
 */
public class Ounce implements Unit {

    public static final String NAME = "Ounce";
    public static final Dimension DIMENSION = Dimension.MASS;
    private static final Ounce instance = new Ounce();

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
        return 0.0283495;
    }

    public static Ounce getInstance() {
        return instance;
    }

}

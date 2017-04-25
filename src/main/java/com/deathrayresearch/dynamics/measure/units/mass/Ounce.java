package com.deathrayresearch.dynamics.measure.units.mass;

import com.deathrayresearch.dynamics.measure.Dimension;
import com.deathrayresearch.dynamics.measure.Unit;
import com.deathrayresearch.dynamics.measure.dimension.Mass;

/**
 *
 */
public class Ounce implements Unit<Mass> {

    public static final String NAME = "Ounce";
    public static final Dimension DIMENSION = Mass.getInstance();
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

package com.deathrayresearch.dynamics.measure.units.mass;

import com.deathrayresearch.dynamics.measure.Dimension;
import com.deathrayresearch.dynamics.measure.Unit;
import com.deathrayresearch.dynamics.measure.dimension.Mass;

/**
 *
 */
public class Pound implements Unit<Mass> {

    public static final String NAME = "Pound";
    public static final Dimension DIMENSION = Mass.getInstance();
    private static final Pound instance = new Pound();

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
        return 0.453592;
    }

    public static Pound getInstance() {
        return instance;
    }

}

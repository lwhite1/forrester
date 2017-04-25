package com.deathrayresearch.dynamics.measure.units.length;

import com.deathrayresearch.dynamics.measure.Dimension;
import com.deathrayresearch.dynamics.measure.Unit;
import com.deathrayresearch.dynamics.measure.dimension.Length;

/**
 *
 */
public class Foot implements Unit<Length> {

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

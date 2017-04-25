package com.deathrayresearch.dynamics.measure.units.length;

import com.deathrayresearch.dynamics.measure.Dimension;
import com.deathrayresearch.dynamics.measure.Unit;
import com.deathrayresearch.dynamics.measure.dimension.Length;

/**
 *
 */
public class Inch implements Unit<Length> {

    public static final String NAME = "Inch";
    public static final Dimension DIMENSION = Length.getInstance();
    private static final Inch instance = new Inch();

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
        return 0.025399936868299999304;
    }
    public static Inch getInstance() {
        return instance;
    }
}

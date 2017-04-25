package com.deathrayresearch.dynamics.measure.units.mass;

import com.deathrayresearch.dynamics.measure.Dimension;
import com.deathrayresearch.dynamics.measure.Unit;
import com.deathrayresearch.dynamics.measure.dimension.Mass;
import com.deathrayresearch.dynamics.measure.dimension.Time;
import com.deathrayresearch.dynamics.measure.units.money.USD;

/**
 *
 */
public class Kilogram implements Unit<Mass> {

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

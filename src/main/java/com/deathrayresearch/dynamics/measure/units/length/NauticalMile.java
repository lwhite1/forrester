package com.deathrayresearch.dynamics.measure.units.length;

import com.deathrayresearch.dynamics.measure.Dimension;
import com.deathrayresearch.dynamics.measure.Unit;
import com.deathrayresearch.dynamics.measure.dimension.Length;

/**
 *
 */
public class NauticalMile implements Unit<Length> {

    public static final String NAME = "Nautical Mile";
    public static final Dimension DIMENSION = Length.getInstance();
    private static final NauticalMile instance = new NauticalMile();

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
        return 1851.9953968539998641;
    }
    public static NauticalMile getInstance() {
        return instance;
    }
}

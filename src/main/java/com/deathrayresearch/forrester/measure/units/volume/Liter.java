package com.deathrayresearch.forrester.measure.units.volume;

import com.deathrayresearch.forrester.measure.Dimension;
import com.deathrayresearch.forrester.measure.Unit;
import com.deathrayresearch.forrester.measure.dimension.Volume;

/**
 *
 */
public class Liter implements Unit {

    public static final String NAME = "Liter";
    public static final Dimension DIMENSION = Volume.getInstance();

    private static final Liter instance = new Liter();

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

    public static Liter getInstance() {
        return instance;
    }


}

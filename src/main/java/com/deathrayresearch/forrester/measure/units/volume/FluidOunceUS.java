package com.deathrayresearch.forrester.measure.units.volume;


import com.deathrayresearch.forrester.measure.Dimension;
import com.deathrayresearch.forrester.measure.Unit;
import com.deathrayresearch.forrester.measure.dimension.Volume;

/**
 *
 */
public class FluidOunceUS implements Unit {

    public static final String NAME = "Fluid ounce (US)";
    public static final Dimension DIMENSION = Dimension.VOLUME;

    private static final FluidOunceUS instance = new FluidOunceUS();

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
        return 0.0295735;
    }

    public static FluidOunceUS getInstance() {
        return instance;
    }


}

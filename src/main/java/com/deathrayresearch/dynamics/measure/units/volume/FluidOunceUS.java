package com.deathrayresearch.dynamics.measure.units.volume;

import com.deathrayresearch.dynamics.measure.Dimension;
import com.deathrayresearch.dynamics.measure.Unit;
import com.deathrayresearch.dynamics.measure.dimension.Volume;

/**
 *
 */
public class FluidOunceUS implements Unit<Volume> {

    public static final String NAME = "Fluid ounce (US)";
    public static final Dimension DIMENSION = Volume.getInstance();

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

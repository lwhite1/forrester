package com.deathrayresearch.dynamics.measure.units.volume;

import com.deathrayresearch.dynamics.measure.Dimension;
import com.deathrayresearch.dynamics.measure.Unit;
import com.deathrayresearch.dynamics.measure.dimension.Volume;

/**
 *
 */
public class CubicMeter implements Unit<Volume> {

    public static final String NAME = "Cubic meter";
    public static final Dimension DIMENSION = Volume.getInstance();

    private static final CubicMeter instance = new CubicMeter();

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
        return 1000.0;
    }

    public static CubicMeter getInstance() {
        return instance;
    }


}

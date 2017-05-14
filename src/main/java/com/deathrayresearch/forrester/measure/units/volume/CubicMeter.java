package com.deathrayresearch.forrester.measure.units.volume;


import com.deathrayresearch.forrester.measure.Dimension;
import com.deathrayresearch.forrester.measure.Unit;
import com.deathrayresearch.forrester.measure.dimension.Volume;

/**
 *
 */
public class CubicMeter implements Unit {

    public static final String NAME = "Cubic meter";
    public static final Dimension DIMENSION = Dimension.VOLUME;

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

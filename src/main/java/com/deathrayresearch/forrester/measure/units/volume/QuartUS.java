package com.deathrayresearch.forrester.measure.units.volume;


import com.deathrayresearch.forrester.measure.Dimension;
import com.deathrayresearch.forrester.measure.Unit;
import com.deathrayresearch.forrester.measure.dimension.Volume;

/**
 *
 */
public class QuartUS implements Unit {

    public static final String NAME = "Quart (US)";
    public static final Dimension DIMENSION = Dimension.VOLUME;

    private static final QuartUS instance = new QuartUS();

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
        return 0.946353;
    }

    public static QuartUS getInstance() {
        return instance;
    }


}

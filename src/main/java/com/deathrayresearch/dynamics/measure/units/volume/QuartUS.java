package com.deathrayresearch.dynamics.measure.units.volume;

import com.deathrayresearch.dynamics.measure.Dimension;
import com.deathrayresearch.dynamics.measure.Unit;
import com.deathrayresearch.dynamics.measure.dimension.Volume;

/**
 *
 */
public class QuartUS implements Unit<Volume> {

    public static final String NAME = "Quart (US)";
    public static final Dimension DIMENSION = Volume.getInstance();

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

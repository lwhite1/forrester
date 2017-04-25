package com.deathrayresearch.dynamics.measure.units.volume;

import com.deathrayresearch.dynamics.measure.Dimension;
import com.deathrayresearch.dynamics.measure.Unit;
import com.deathrayresearch.dynamics.measure.dimension.Volume;

/**
 *
 */
public class GallonUS implements Unit<Volume> {

    public static final String NAME = "Gallon (US)";
    public static final Dimension DIMENSION = Volume.getInstance();

    private static final GallonUS instance = new GallonUS();

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
        return 3.78541;
    }

    public static GallonUS getInstance() {
        return instance;
    }


}

package com.deathrayresearch.forrester.measure.units.volume;


import com.deathrayresearch.forrester.measure.Dimension;
import com.deathrayresearch.forrester.measure.Unit;
import com.deathrayresearch.forrester.measure.dimension.Volume;

/**
 *
 */
public class GallonUS implements Unit {

    public static final String NAME = "Gallon (US)";
    public static final Dimension DIMENSION = Dimension.VOLUME;

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

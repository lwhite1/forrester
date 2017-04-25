package com.deathrayresearch.dynamics.measure.dimension;

import com.deathrayresearch.dynamics.measure.Converter;
import com.deathrayresearch.dynamics.measure.Dimension;
import com.deathrayresearch.dynamics.measure.Unit;
import com.deathrayresearch.dynamics.measure.units.volume.Liter;

/**
 *
 */
public class Volume implements Dimension {

    private static Volume ourInstance = new Volume();

    public static Volume getInstance() {
        return ourInstance;
    }

    @Override
    public Converter getConverter() {
        return null;
    }

    @Override
    public Unit getBaseUnit() {
        return Liter.getInstance();
    }
}

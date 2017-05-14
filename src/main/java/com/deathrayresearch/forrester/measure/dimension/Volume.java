package com.deathrayresearch.forrester.measure.dimension;


import com.deathrayresearch.forrester.measure.Converter;
import com.deathrayresearch.forrester.measure.Dimension;
import com.deathrayresearch.forrester.measure.Unit;
import com.deathrayresearch.forrester.measure.units.volume.Liter;

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

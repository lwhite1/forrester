package com.deathrayresearch.forrester.measure.dimension;

import com.deathrayresearch.forrester.measure.Converter;
import com.deathrayresearch.forrester.measure.Dimension;
import com.deathrayresearch.forrester.measure.Unit;
import com.deathrayresearch.forrester.measure.units.mass.Kilogram;

/**
 *
 */
public class Mass implements Dimension {

    private static Mass ourInstance = new Mass();

    public static Mass getInstance() {
        return ourInstance;
    }

    @Override
    public Converter getConverter() {
        return null;
    }

    @Override
    public Unit getBaseUnit() {
        return Kilogram.getInstance();
    }
}

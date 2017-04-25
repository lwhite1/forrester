package com.deathrayresearch.dynamics.measure.dimension;

import com.deathrayresearch.dynamics.measure.Converter;
import com.deathrayresearch.dynamics.measure.Dimension;
import com.deathrayresearch.dynamics.measure.Unit;
import com.deathrayresearch.dynamics.measure.units.mass.Kilogram;
import com.deathrayresearch.dynamics.measure.units.volume.Liter;

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

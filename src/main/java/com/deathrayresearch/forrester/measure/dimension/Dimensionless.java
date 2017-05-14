package com.deathrayresearch.forrester.measure.dimension;

import com.deathrayresearch.forrester.measure.Converter;
import com.deathrayresearch.forrester.measure.Dimension;
import com.deathrayresearch.forrester.measure.Unit;

/**
 * Represents a "dimensionless" dimension (like a pure fraction)
 */
public class Dimensionless implements Dimension {

    private static Dimensionless ourInstance = new Dimensionless();

    public static Dimensionless getInstance() {
        return ourInstance;
    }

    @Override
    public Converter getConverter() {
        return null;
    }

    @Override
    public Unit getBaseUnit() {
        return null;
    }
}

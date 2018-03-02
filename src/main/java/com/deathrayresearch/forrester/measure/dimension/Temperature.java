package com.deathrayresearch.forrester.measure.dimension;

import com.deathrayresearch.forrester.measure.Converter;
import com.deathrayresearch.forrester.measure.Dimension;
import com.deathrayresearch.forrester.measure.Unit;
import com.deathrayresearch.forrester.measure.units.temperature.Centigrade;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 *
 */
public class Temperature implements Dimension {

    private static Temperature ourInstance = new Temperature();

    public static Temperature getInstance() {
        return ourInstance;
    }

    private Temperature() {}

    @Override
    public Unit getBaseUnit() {
        return Centigrade.getInstance();
    }

    @Override
    public Converter getConverter() {
        throw new UnsupportedOperationException("There is no converter (yet) for temperatures");
    }
}

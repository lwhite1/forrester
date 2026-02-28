package com.deathrayresearch.forrester.measure.dimension;

import com.deathrayresearch.forrester.measure.Converter;
import com.deathrayresearch.forrester.measure.Dimension;
import com.deathrayresearch.forrester.measure.Unit;
import com.deathrayresearch.forrester.measure.units.temperature.TemperatureUnits;

/**
 * The temperature dimension. Base unit is centigrade (Celsius).
 * Unit conversion is not yet supported for temperature.
 */
public enum Temperature implements Dimension {

    INSTANCE;

    public static Temperature getInstance() {
        return INSTANCE;
    }

    @Override
    public Unit getBaseUnit() {
        return TemperatureUnits.CENTIGRADE;
    }

    @Override
    public Converter getConverter() {
        throw new UnsupportedOperationException("There is no converter (yet) for temperatures");
    }
}

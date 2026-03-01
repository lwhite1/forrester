package com.deathrayresearch.forrester.measure.dimension;

import com.deathrayresearch.forrester.measure.Converter;
import com.deathrayresearch.forrester.measure.Dimension;
import com.deathrayresearch.forrester.measure.Unit;
import com.deathrayresearch.forrester.measure.units.temperature.TemperatureUnits;

/**
 * The temperature dimension. Base unit is Celsius.
 *
 * <p>Unit conversion between Celsius and Fahrenheit is not supported because it requires
 * an affine transformation (offset + scale) that the ratio-based converter cannot express.
 */
public enum Temperature implements Dimension {

    INSTANCE;

    @Override
    public Unit getBaseUnit() {
        return TemperatureUnits.CELSIUS;
    }

    @Override
    public Converter getConverter() {
        throw new UnsupportedOperationException("There is no converter (yet) for temperatures");
    }
}

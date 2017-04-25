package com.deathrayresearch.dynamics.measure.dimension;

import com.deathrayresearch.dynamics.measure.Converter;
import com.deathrayresearch.dynamics.measure.Dimension;
import com.deathrayresearch.dynamics.measure.Quantity;
import com.deathrayresearch.dynamics.measure.Unit;
import com.deathrayresearch.dynamics.measure.units.length.Meter;

/**
 *
 */
public class Length implements Dimension {

    private Converter<Length> converter = new Converter<Length>() {

        @Override
        public Quantity<Length> convert(Quantity<Length> originalQuantity, Unit<Length> newUnit) {
            Quantity<Length> inBaseUnits = originalQuantity.inBaseUnits();
            return newUnit.fromBaseUnits(inBaseUnits);
        }
    };

    private static Length ourInstance = new Length();

    public static Length getInstance() {
        return ourInstance;
    }

    private Length() {}

    @Override
    public Unit getBaseUnit() {
        return Meter.getInstance();
    }

    @Override
    public Converter<Length> getConverter() {
        return converter;
    }
}

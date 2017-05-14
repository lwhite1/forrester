package com.deathrayresearch.forrester.measure.dimension;


import com.deathrayresearch.forrester.measure.Converter;
import com.deathrayresearch.forrester.measure.Dimension;
import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.Unit;
import com.deathrayresearch.forrester.measure.units.length.Meter;

/**
 *
 */
public class Length implements Dimension {

    private Converter converter = new Converter() {

        @Override
        public Quantity convert(Quantity originalQuantity, Unit newUnit) {
            Quantity inBaseUnits = originalQuantity.inBaseUnits();
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
    public Converter getConverter() {
        return converter;
    }
}

package com.deathrayresearch.forrester.measure.dimension;


import com.deathrayresearch.forrester.measure.Converter;
import com.deathrayresearch.forrester.measure.Dimension;
import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.Unit;
import com.deathrayresearch.forrester.measure.units.length.Meter;
import com.google.common.base.Preconditions;

/**
 *
 */
public class Length implements Dimension {

    private Converter converter = (originalQuantity, newUnit) -> {
        Preconditions.checkArgument(newUnit.getDimension().equals(Length.getInstance()));
        Quantity inBaseUnits = originalQuantity.inBaseUnits();
        return newUnit.fromBaseUnits(inBaseUnits);
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

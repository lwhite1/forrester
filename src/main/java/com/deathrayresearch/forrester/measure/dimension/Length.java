package com.deathrayresearch.forrester.measure.dimension;


import com.deathrayresearch.forrester.measure.Dimension;
import com.deathrayresearch.forrester.measure.Unit;
import com.deathrayresearch.forrester.measure.units.length.Meter;

/**
 *
 */
public class Length implements Dimension {

    private static final Length ourInstance = new Length();

    public static Length getInstance() {
        return ourInstance;
    }

    private Length() {}

    @Override
    public Unit getBaseUnit() {
        return Meter.getInstance();
    }
}

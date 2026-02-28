package com.deathrayresearch.forrester.measure.dimension;

import com.deathrayresearch.forrester.measure.Dimension;
import com.deathrayresearch.forrester.measure.Unit;
import com.deathrayresearch.forrester.measure.units.length.LengthUnits;

public enum Length implements Dimension {

    INSTANCE;

    public static Length getInstance() {
        return INSTANCE;
    }

    @Override
    public Unit getBaseUnit() {
        return LengthUnits.METER;
    }
}

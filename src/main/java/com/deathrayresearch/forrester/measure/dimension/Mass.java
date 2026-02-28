package com.deathrayresearch.forrester.measure.dimension;

import com.deathrayresearch.forrester.measure.Dimension;
import com.deathrayresearch.forrester.measure.Unit;
import com.deathrayresearch.forrester.measure.units.mass.MassUnits;

public enum Mass implements Dimension {

    INSTANCE;

    public static Mass getInstance() {
        return INSTANCE;
    }

    @Override
    public Unit getBaseUnit() {
        return MassUnits.KILOGRAM;
    }
}

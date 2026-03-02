package com.deathrayresearch.forrester.measure.dimension;

import com.deathrayresearch.forrester.measure.Dimension;
import com.deathrayresearch.forrester.measure.Unit;
import com.deathrayresearch.forrester.measure.units.mass.MassUnits;

/**
 * The mass dimension. Base unit is kilograms.
 */
public enum Mass implements Dimension {

    INSTANCE;

    @Override
    public Unit getBaseUnit() {
        return MassUnits.KILOGRAM;
    }
}

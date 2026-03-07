package com.deathrayresearch.forrester.measure.dimension;

import com.deathrayresearch.forrester.measure.Dimension;
import com.deathrayresearch.forrester.measure.Unit;
import com.deathrayresearch.forrester.measure.units.length.LengthUnits;

/**
 * The length dimension. Base unit is meters.
 */
public enum Length implements Dimension {

    INSTANCE;

    /** {@inheritDoc} Returns {@link LengthUnits#METER}. */
    @Override
    public Unit getBaseUnit() {
        return LengthUnits.METER;
    }
}

package com.deathrayresearch.forrester.measure.dimension;

import com.deathrayresearch.forrester.measure.Dimension;
import com.deathrayresearch.forrester.measure.Unit;
import com.deathrayresearch.forrester.measure.units.time.TimeUnits;

/**
 * The time dimension. Base unit is seconds.
 */
public enum Time implements Dimension {

    INSTANCE;

    @Override
    public Unit getBaseUnit() {
        return TimeUnits.SECOND;
    }
}

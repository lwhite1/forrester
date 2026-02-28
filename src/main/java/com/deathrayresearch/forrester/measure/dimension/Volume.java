package com.deathrayresearch.forrester.measure.dimension;

import com.deathrayresearch.forrester.measure.Dimension;
import com.deathrayresearch.forrester.measure.Unit;
import com.deathrayresearch.forrester.measure.units.volume.VolumeUnits;

/**
 * The volume dimension. Base unit is liters.
 */
public enum Volume implements Dimension {

    INSTANCE;

    @Override
    public Unit getBaseUnit() {
        return VolumeUnits.LITER;
    }
}

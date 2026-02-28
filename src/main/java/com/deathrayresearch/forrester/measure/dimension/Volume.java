package com.deathrayresearch.forrester.measure.dimension;

import com.deathrayresearch.forrester.measure.Dimension;
import com.deathrayresearch.forrester.measure.Unit;
import com.deathrayresearch.forrester.measure.units.volume.VolumeUnits;

public enum Volume implements Dimension {

    INSTANCE;

    public static Volume getInstance() {
        return INSTANCE;
    }

    @Override
    public Unit getBaseUnit() {
        return VolumeUnits.LITER;
    }
}

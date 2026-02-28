package com.deathrayresearch.forrester.measure.units.length;

import com.deathrayresearch.forrester.measure.Dimension;
import com.deathrayresearch.forrester.measure.Unit;

public enum LengthUnits implements Unit {

    METER("Meter", 1.0),
    FOOT("Foot", 0.3047992424196),
    INCH("Inch", 0.025399936868299999304),
    MILE("Mile", 1609.34),
    NAUTICAL_MILE("Nautical Mile", 1851.9953968539998641);

    private final String name;
    private final double ratioToBaseUnit;

    LengthUnits(String name, double ratioToBaseUnit) {
        this.name = name;
        this.ratioToBaseUnit = ratioToBaseUnit;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Dimension getDimension() {
        return Dimension.LENGTH;
    }

    @Override
    public double ratioToBaseUnit() {
        return ratioToBaseUnit;
    }
}

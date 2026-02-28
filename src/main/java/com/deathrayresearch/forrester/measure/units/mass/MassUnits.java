package com.deathrayresearch.forrester.measure.units.mass;

import com.deathrayresearch.forrester.measure.Dimension;
import com.deathrayresearch.forrester.measure.Unit;

public enum MassUnits implements Unit {

    KILOGRAM("Kilogram", 1.0),
    POUND("Pound", 0.453592),
    OUNCE("Ounce", 0.0283495);

    private final String name;
    private final double ratioToBaseUnit;

    MassUnits(String name, double ratioToBaseUnit) {
        this.name = name;
        this.ratioToBaseUnit = ratioToBaseUnit;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Dimension getDimension() {
        return Dimension.MASS;
    }

    @Override
    public double ratioToBaseUnit() {
        return ratioToBaseUnit;
    }
}

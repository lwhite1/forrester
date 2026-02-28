package com.deathrayresearch.forrester.measure.units.volume;

import com.deathrayresearch.forrester.measure.Dimension;
import com.deathrayresearch.forrester.measure.Unit;

public enum VolumeUnits implements Unit {

    LITER("Liter", 1.0),
    GALLON_US("Gallon (US)", 3.78541),
    CUBIC_METER("Cubic meter", 1000.0),
    FLUID_OUNCE_US("Fluid ounce (US)", 0.0295735),
    QUART_US("Quart (US)", 0.946353);

    private final String name;
    private final double ratioToBaseUnit;

    VolumeUnits(String name, double ratioToBaseUnit) {
        this.name = name;
        this.ratioToBaseUnit = ratioToBaseUnit;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Dimension getDimension() {
        return Dimension.VOLUME;
    }

    @Override
    public double ratioToBaseUnit() {
        return ratioToBaseUnit;
    }
}

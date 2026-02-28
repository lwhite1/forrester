package com.deathrayresearch.forrester.measure.units.temperature;

import com.deathrayresearch.forrester.measure.Dimension;
import com.deathrayresearch.forrester.measure.Unit;

/**
 * Units of temperature. Currently only centigrade (Celsius) is defined.
 */
public enum TemperatureUnits implements Unit {

    CENTIGRADE("Centigrade", 1.0);

    private final String name;
    private final double ratioToBaseUnit;

    TemperatureUnits(String name, double ratioToBaseUnit) {
        this.name = name;
        this.ratioToBaseUnit = ratioToBaseUnit;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Dimension getDimension() {
        return Dimension.TEMPERATURE;
    }

    @Override
    public double ratioToBaseUnit() {
        return ratioToBaseUnit;
    }
}

package com.deathrayresearch.forrester.measure.units.temperature;

import com.deathrayresearch.forrester.measure.Dimension;
import com.deathrayresearch.forrester.measure.Unit;

/**
 * Units of temperature. Celsius is the base unit.
 *
 * <p>Note: Celsius/Fahrenheit conversion involves an offset (F = C * 9/5 + 32) that cannot
 * be expressed as a simple ratio. The {@code ratioToBaseUnit} for Fahrenheit reflects the
 * degree-size relationship (1 °F = 5/9 °C interval), which is correct for temperature
 * <em>differences</em> but not absolute values. Direct conversion between Celsius and
 * Fahrenheit quantities is not supported; use a single temperature unit per model.
 */
public enum TemperatureUnits implements Unit {

    CELSIUS("Celsius", 1.0),
    FAHRENHEIT("Fahrenheit", 5.0 / 9.0);

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

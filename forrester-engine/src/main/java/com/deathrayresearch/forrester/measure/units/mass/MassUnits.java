package com.deathrayresearch.forrester.measure.units.mass;

import com.deathrayresearch.forrester.measure.Dimension;
import com.deathrayresearch.forrester.measure.Unit;

/**
 * Standard units of mass. Each constant stores its ratio to the base unit (kilograms).
 * Conversion factors use the exact international definitions where applicable.
 */
public enum MassUnits implements Unit {

    MILLIGRAM("Milligram", 0.000001),
    GRAM("Gram", 0.001),
    KILOGRAM("Kilogram", 1.0),
    METRIC_TON("Metric Ton", 1000.0),
    OUNCE("Ounce", 0.028349523125),
    POUND("Pound", 0.45359237),
    SHORT_TON("Short Ton", 907.18474);

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

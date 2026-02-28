package com.deathrayresearch.forrester.measure.units.mass;

import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.Unit;

public final class Masses {

    public static Quantity kilograms(double value) {
        Unit unit = MassUnits.KILOGRAM;
        return new Quantity(value, unit);
    }

    public static Quantity pounds(double value) {
        Unit unit = MassUnits.POUND;
        return new Quantity(value, unit);
    }

    public static Quantity ounces(double value) {
        Unit unit = MassUnits.OUNCE;
        return new Quantity(value, unit);
    }

    private Masses() {}
}

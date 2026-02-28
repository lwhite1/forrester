package com.deathrayresearch.forrester.measure.units.length;

import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.Unit;

public final class Lengths {

    public static Quantity feet(double value) {
        Unit unit = LengthUnits.FOOT;
        return new Quantity(value, unit);
    }

    public static Quantity inches(double value) {
        Unit unit = LengthUnits.INCH;
        return new Quantity(value, unit);
    }

    public static Quantity meters(double value) {
        Unit unit = LengthUnits.METER;
        return new Quantity(value, unit);
    }

    public static Quantity miles(double value) {
        Unit unit = LengthUnits.MILE;
        return new Quantity(value, unit);
    }

    public static Quantity nauticalMiles(double value) {
        Unit unit = LengthUnits.NAUTICAL_MILE;
        return new Quantity(value, unit);
    }
}

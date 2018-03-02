package com.deathrayresearch.forrester.measure.units.length;


import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.Unit;

/**
 *
 */
public final class Lengths {

    public static Quantity feet(double value) {
        Unit unit = Foot.getInstance();
        return new Quantity(value, unit);
    }

    public static Quantity inches( double value) {
        Unit unit = Inch.getInstance();
        return new Quantity(value, unit);
    }

    public static Quantity meters(double value) {
        Unit unit = Meter.getInstance();
        return new Quantity(value, unit);
    }

    public static Quantity miles(double value) {
        Unit unit = Mile.getInstance();
        return new Quantity(value, unit);
    }

    public static Quantity nauticalMiles(double value) {
        Unit unit = NauticalMile.getInstance();
        return new Quantity(value, unit);
    }
}

package com.deathrayresearch.forrester.measure.units.length;


import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.Unit;

/**
 *
 */
public final class Lengths {

    public static Quantity feet(String name, double value) {
        Unit unit = Foot.getInstance();
        return new Quantity(name, value, unit);
    }

    public static Quantity inches(String name, double value) {
        Unit unit = Inch.getInstance();
        return new Quantity(name, value, unit);
    }

    public static Quantity meters(String name, double value) {
        Unit unit = Meter.getInstance();
        return new Quantity(name, value, unit);
    }

    public static Quantity miles(String name, double value) {
        Unit unit = Mile.getInstance();
        return new Quantity(name, value, unit);
    }

    public static Quantity nauticalMiles(String name, double value) {
        Unit unit = NauticalMile.getInstance();
        return new Quantity(name, value, unit);
    }
}

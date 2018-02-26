package com.deathrayresearch.forrester.measure.units.mass;

import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.Unit;

/**
 *
 */
public final class Masses {

    public static Quantity kilograms(String name, double value) {
        Unit unit = Kilogram.getInstance();
        return new Quantity(name, value, unit);
    }

    public static Quantity pounds(String name, double value) {
        Unit unit = Pound.getInstance();
        return new Quantity(name, value, unit);
    }

    public static Quantity ounces(String name, double value) {
        Unit unit = Ounce.getInstance();
        return new Quantity(name, value, unit);
    }

    private Masses() {}
}

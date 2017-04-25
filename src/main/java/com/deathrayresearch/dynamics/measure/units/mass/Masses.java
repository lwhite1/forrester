package com.deathrayresearch.dynamics.measure.units.mass;

import com.deathrayresearch.dynamics.measure.Quantity;
import com.deathrayresearch.dynamics.measure.Unit;
import com.deathrayresearch.dynamics.measure.dimension.Mass;

/**
 *
 */
public final class Masses {

    public static Quantity<Mass> kilograms(double value) {
        Unit<Mass> unit = Kilogram.getInstance();
        return new Quantity<>(value, unit);
    }

    public static Quantity<Mass> pounds(double value) {
        Unit<Mass> unit = Pound.getInstance();
        return new Quantity<>(value, unit);
    }

    public static Quantity<Mass> ounces(double value) {
        Unit<Mass> unit = Ounce.getInstance();
        return new Quantity<>(value, unit);
    }

    private Masses() {}
}

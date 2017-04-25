package com.deathrayresearch.dynamics.measure.units.length;

import com.deathrayresearch.dynamics.measure.Quantity;
import com.deathrayresearch.dynamics.measure.Unit;
import com.deathrayresearch.dynamics.measure.dimension.Length;

/**
 *
 */
public final class Lengths {

    public static Quantity<Length> feet(double value) {
        Unit<Length> unit = Foot.getInstance();
        return new Quantity<>(value, unit);
    }

    public static Quantity<Length> inches(double value) {
        Unit<Length> unit = Inch.getInstance();
        return new Quantity<>(value, unit);
    }

    public static Quantity<Length> meters(double value) {
        Unit<Length> unit = Meter.getInstance();
        return new Quantity<>(value, unit);
    }

    public static Quantity<Length> miles(double value) {
        Unit<Length> unit = Mile.getInstance();
        return new Quantity<>(value, unit);
    }

    public static Quantity<Length> nauticalMiles(double value) {
        Unit<Length> unit = NauticalMile.getInstance();
        return new Quantity<>(value, unit);
    }
}

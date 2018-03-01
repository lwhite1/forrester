package com.deathrayresearch.forrester.measure.units.volume;


import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.Unit;

/**
 *
 */
public final class Volumes {

    public static Quantity liters(double value) {
        Unit unit = Liter.getInstance();
        return new Quantity(value, unit);
    }

    public static Quantity gallonsUS(double value) {
        Unit unit = GallonUS.getInstance();
        return new Quantity(value, unit);
    }

    public static Quantity cubicMeters(double value) {
        Unit unit = CubicMeter.getInstance();
        return new Quantity(value, unit);
    }

    public static Quantity fluidOuncesUS(double value) {
        Unit unit = FluidOunceUS.getInstance();
        return new Quantity(value, unit);
    }

    public static Quantity quartsUS(double value) {
        Unit unit = QuartUS.getInstance();
        return new Quantity(value, unit);
    }

    private Volumes() {}
}

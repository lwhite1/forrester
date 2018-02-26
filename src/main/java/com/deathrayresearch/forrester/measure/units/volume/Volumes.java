package com.deathrayresearch.forrester.measure.units.volume;


import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.Unit;

/**
 *
 */
public final class Volumes {

    public static Quantity liters(String name, double value) {
        Unit unit = Liter.getInstance();
        return new Quantity(name, value, unit);
    }

    public static Quantity gallonsUS(String name, double value) {
        Unit unit = GallonUS.getInstance();
        return new Quantity(name, value, unit);
    }

    public static Quantity cubicMeters(String name, double value) {
        Unit unit = CubicMeter.getInstance();
        return new Quantity(name, value, unit);
    }

    public static Quantity fluidOuncesUS(String name, double value) {
        Unit unit = FluidOunceUS.getInstance();
        return new Quantity(name, value, unit);
    }

    public static Quantity quartsUS(String name, double value) {
        Unit unit = QuartUS.getInstance();
        return new Quantity(name, value, unit);
    }

    private Volumes() {}
}

package com.deathrayresearch.forrester.measure.units.volume;

import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.Unit;

public final class Volumes {

    public static Quantity liters(double value) {
        Unit unit = VolumeUnits.LITER;
        return new Quantity(value, unit);
    }

    public static Quantity gallonsUS(double value) {
        Unit unit = VolumeUnits.GALLON_US;
        return new Quantity(value, unit);
    }

    public static Quantity cubicMeters(double value) {
        Unit unit = VolumeUnits.CUBIC_METER;
        return new Quantity(value, unit);
    }

    public static Quantity fluidOuncesUS(double value) {
        Unit unit = VolumeUnits.FLUID_OUNCE_US;
        return new Quantity(value, unit);
    }

    public static Quantity quartsUS(double value) {
        Unit unit = VolumeUnits.QUART_US;
        return new Quantity(value, unit);
    }

    private Volumes() {}
}

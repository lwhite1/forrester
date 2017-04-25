package com.deathrayresearch.dynamics.measure.units.volume;

import com.deathrayresearch.dynamics.measure.Quantity;
import com.deathrayresearch.dynamics.measure.Unit;
import com.deathrayresearch.dynamics.measure.dimension.Volume;

/**
 *
 */
public final class Volumes {

    public static Quantity<Volume> liters(double value) {
        Unit<Volume> unit = Liter.getInstance();
        return new Quantity<>(value, unit);
    }

    public static Quantity<Volume> gallonsUS(double value) {
        Unit<Volume> unit = GallonUS.getInstance();
        return new Quantity<>(value, unit);
    }

    public static Quantity<Volume> cubicMeters(double value) {
        Unit<Volume> unit = CubicMeter.getInstance();
        return new Quantity<>(value, unit);
    }

    public static Quantity<Volume> fluidOuncesUS(double value) {
        Unit<Volume> unit = FluidOunceUS.getInstance();
        return new Quantity<>(value, unit);
    }

    public static Quantity<Volume> quartsUS(double value) {
        Unit<Volume> unit = QuartUS.getInstance();
        return new Quantity<>(value, unit);
    }

    private Volumes() {}
}

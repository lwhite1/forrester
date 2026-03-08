package systems.courant.forrester.measure.units.volume;

import systems.courant.forrester.measure.Quantity;
import systems.courant.forrester.measure.Unit;

/**
 * Factory methods for creating volume {@link Quantity} instances in various volume units.
 */
public final class Volumes {

    /**
     * Creates a quantity of the given value in liters.
     */
    public static Quantity liters(double value) {
        Unit unit = VolumeUnits.LITER;
        return new Quantity(value, unit);
    }

    /**
     * Creates a quantity of the given value in US gallons.
     */
    public static Quantity gallonsUS(double value) {
        Unit unit = VolumeUnits.GALLON_US;
        return new Quantity(value, unit);
    }

    /**
     * Creates a quantity of the given value in cubic meters.
     */
    public static Quantity cubicMeters(double value) {
        Unit unit = VolumeUnits.CUBIC_METER;
        return new Quantity(value, unit);
    }

    /**
     * Creates a quantity of the given value in US fluid ounces.
     */
    public static Quantity fluidOuncesUS(double value) {
        Unit unit = VolumeUnits.FLUID_OUNCE_US;
        return new Quantity(value, unit);
    }

    /**
     * Creates a quantity of the given value in US quarts.
     */
    public static Quantity quartsUS(double value) {
        Unit unit = VolumeUnits.QUART_US;
        return new Quantity(value, unit);
    }

    private Volumes() {}
}

package systems.courant.forrester.measure.units.length;

import systems.courant.forrester.measure.Quantity;
import systems.courant.forrester.measure.Unit;

/**
 * Factory methods for creating length {@link Quantity} instances in various length units.
 */
public final class Lengths {

    /**
     * Creates a quantity of the given value in feet.
     */
    public static Quantity feet(double value) {
        Unit unit = LengthUnits.FOOT;
        return new Quantity(value, unit);
    }

    /**
     * Creates a quantity of the given value in inches.
     */
    public static Quantity inches(double value) {
        Unit unit = LengthUnits.INCH;
        return new Quantity(value, unit);
    }

    /**
     * Creates a quantity of the given value in meters.
     */
    public static Quantity meters(double value) {
        Unit unit = LengthUnits.METER;
        return new Quantity(value, unit);
    }

    /**
     * Creates a quantity of the given value in miles.
     */
    public static Quantity miles(double value) {
        Unit unit = LengthUnits.MILE;
        return new Quantity(value, unit);
    }

    /**
     * Creates a quantity of the given value in nautical miles.
     */
    public static Quantity nauticalMiles(double value) {
        Unit unit = LengthUnits.NAUTICAL_MILE;
        return new Quantity(value, unit);
    }
}

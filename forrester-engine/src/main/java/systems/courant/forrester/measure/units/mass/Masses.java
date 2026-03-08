package systems.courant.forrester.measure.units.mass;

import systems.courant.forrester.measure.Quantity;
import systems.courant.forrester.measure.Unit;

/**
 * Factory methods for creating mass {@link Quantity} instances in various mass units.
 */
public final class Masses {

    /**
     * Creates a quantity of the given value in kilograms.
     */
    public static Quantity kilograms(double value) {
        Unit unit = MassUnits.KILOGRAM;
        return new Quantity(value, unit);
    }

    /**
     * Creates a quantity of the given value in pounds.
     */
    public static Quantity pounds(double value) {
        Unit unit = MassUnits.POUND;
        return new Quantity(value, unit);
    }

    /**
     * Creates a quantity of the given value in ounces.
     */
    public static Quantity ounces(double value) {
        Unit unit = MassUnits.OUNCE;
        return new Quantity(value, unit);
    }

    private Masses() {}
}

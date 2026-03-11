package systems.courant.forrester.measure.units.mass;

import systems.courant.forrester.measure.Dimension;
import systems.courant.forrester.measure.Unit;

/**
 * Standard units of mass. Each constant stores its ratio to the base unit (kilograms).
 * Conversion factors use the exact international definitions where applicable.
 */
public enum MassUnits implements Unit {

    MILLIGRAM("Milligram", 0.000001),
    GRAM("Gram", 0.001),
    KILOGRAM("Kilogram", 1.0),
    METRIC_TON("Metric Ton", 1000.0),
    OUNCE("Ounce", 0.028349523125),
    POUND("Pound", 0.45359237),
    SHORT_TON("Short Ton", 907.18474);

    private final String name;
    private final double ratioToBaseUnit;

    MassUnits(String name, double ratioToBaseUnit) {
        this.name = name;
        this.ratioToBaseUnit = ratioToBaseUnit;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return name;
    }

    /** {@inheritDoc} Returns {@link Dimension#MASS}. */
    @Override
    public Dimension getDimension() {
        return Dimension.MASS;
    }

    /** {@inheritDoc} */
    @Override
    public double ratioToBaseUnit() {
        return ratioToBaseUnit;
    }
}

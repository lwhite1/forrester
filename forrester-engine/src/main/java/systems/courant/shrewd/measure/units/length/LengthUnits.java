package systems.courant.forrester.measure.units.length;

import systems.courant.forrester.measure.Dimension;
import systems.courant.forrester.measure.Unit;

/**
 * Standard units of length. Each constant stores its ratio to the base unit (meters).
 * Conversion factors use the exact international definitions where applicable.
 */
public enum LengthUnits implements Unit {

    MILLIMETER("Millimeter", 0.001),
    CENTIMETER("Centimeter", 0.01),
    METER("Meter", 1.0),
    KILOMETER("Kilometer", 1000.0),
    INCH("Inch", 0.0254),
    FOOT("Foot", 0.3048),
    YARD("Yard", 0.9144),
    MILE("Mile", 1609.344),
    NAUTICAL_MILE("Nautical Mile", 1852.0);

    private final String name;
    private final double ratioToBaseUnit;

    LengthUnits(String name, double ratioToBaseUnit) {
        this.name = name;
        this.ratioToBaseUnit = ratioToBaseUnit;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return name;
    }

    /** {@inheritDoc} Returns {@link Dimension#LENGTH}. */
    @Override
    public Dimension getDimension() {
        return Dimension.LENGTH;
    }

    /** {@inheritDoc} */
    @Override
    public double ratioToBaseUnit() {
        return ratioToBaseUnit;
    }
}

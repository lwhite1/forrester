package systems.courant.forrester.measure.units.dimensionless;

import systems.courant.forrester.measure.Dimension;
import systems.courant.forrester.measure.Unit;
import systems.courant.forrester.measure.dimension.Dimensionless;

/**
 * Units for dimensionless quantities (ratios, percentages, pure numbers).
 */
public enum DimensionlessUnits implements Unit {

    DIMENSIONLESS("Dimensionless unit", 1.0);

    private final String name;
    private final double ratioToBaseUnit;

    DimensionlessUnits(String name, double ratioToBaseUnit) {
        this.name = name;
        this.ratioToBaseUnit = ratioToBaseUnit;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return name;
    }

    /** {@inheritDoc} Returns {@link Dimensionless#INSTANCE}. */
    @Override
    public Dimension getDimension() {
        return Dimensionless.INSTANCE;
    }

    /** {@inheritDoc} */
    @Override
    public double ratioToBaseUnit() {
        return ratioToBaseUnit;
    }
}

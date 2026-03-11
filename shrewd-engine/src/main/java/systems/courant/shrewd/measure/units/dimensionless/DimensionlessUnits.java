package systems.courant.shrewd.measure.units.dimensionless;

import systems.courant.shrewd.measure.Dimension;
import systems.courant.shrewd.measure.Unit;
import systems.courant.shrewd.measure.dimension.Dimensionless;

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

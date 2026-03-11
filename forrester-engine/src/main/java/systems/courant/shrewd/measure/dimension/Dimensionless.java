package systems.courant.shrewd.measure.dimension;

import systems.courant.shrewd.measure.Converter;
import systems.courant.shrewd.measure.Dimension;
import systems.courant.shrewd.measure.Unit;
import systems.courant.shrewd.measure.units.dimensionless.DimensionlessUnits;

/**
 * A pseudo-dimension for quantities that have no physical dimension (e.g., ratios, percentages).
 * Unit conversion is not supported for dimensionless quantities.
 */
public enum Dimensionless implements Dimension {

    INSTANCE;

    /**
     * Always throws {@link UnsupportedOperationException} because dimensionless quantities
     * have no meaningful unit conversion.
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    public Converter getConverter() {
        throw new UnsupportedOperationException("Dimensionless quantities have no unit conversion");
    }

    /** {@inheritDoc} Returns {@link DimensionlessUnits#DIMENSIONLESS}. */
    @Override
    public Unit getBaseUnit() {
        return DimensionlessUnits.DIMENSIONLESS;
    }
}

package systems.courant.sd.measure.dimension;

import systems.courant.sd.measure.Converter;
import systems.courant.sd.measure.Dimension;
import systems.courant.sd.measure.Unit;
import systems.courant.sd.measure.units.dimensionless.DimensionlessUnits;

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

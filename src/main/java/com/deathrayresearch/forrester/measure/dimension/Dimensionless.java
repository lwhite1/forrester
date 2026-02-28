package com.deathrayresearch.forrester.measure.dimension;

import com.deathrayresearch.forrester.measure.Converter;
import com.deathrayresearch.forrester.measure.Dimension;
import com.deathrayresearch.forrester.measure.Unit;
import com.deathrayresearch.forrester.measure.units.dimensionless.DimensionlessUnits;

/**
 * A pseudo-dimension for quantities that have no physical dimension (e.g., ratios, percentages).
 * Unit conversion is not supported for dimensionless quantities.
 */
public enum Dimensionless implements Dimension {

    INSTANCE;

    public static Dimensionless getInstance() {
        return INSTANCE;
    }

    @Override
    public Converter getConverter() {
        throw new UnsupportedOperationException("Dimensionless quantities have no unit conversion");
    }

    @Override
    public Unit getBaseUnit() {
        return DimensionlessUnits.DIMENSIONLESS;
    }
}

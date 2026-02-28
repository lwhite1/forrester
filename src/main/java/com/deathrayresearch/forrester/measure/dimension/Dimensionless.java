package com.deathrayresearch.forrester.measure.dimension;

import com.deathrayresearch.forrester.measure.Converter;
import com.deathrayresearch.forrester.measure.Dimension;
import com.deathrayresearch.forrester.measure.Unit;
import com.deathrayresearch.forrester.measure.units.dimensionless.DimensionlessUnits;

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

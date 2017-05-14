package com.deathrayresearch.forrester.measure.units.dimensionless;

import com.deathrayresearch.forrester.measure.Dimension;
import com.deathrayresearch.forrester.measure.Unit;
import com.deathrayresearch.forrester.measure.dimension.Dimensionless;

/**
 *
 */
public class DimensionlessUnit implements Unit {

    private static final DimensionlessUnit instance = new DimensionlessUnit();

    public static DimensionlessUnit getInstance() {
        return instance;
    }

    @Override
    public String getName() {
        return "Dimensionless unit";
    }

    @Override
    public Dimension getDimension() {
        return Dimensionless.getInstance();
    }

    @Override
    public double ratioToBaseUnit() {
        return 1.0;
    }
}

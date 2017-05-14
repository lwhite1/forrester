package com.deathrayresearch.forrester.measure.units.length;

import com.deathrayresearch.forrester.measure.Dimension;
import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.Unit;
import com.deathrayresearch.forrester.measure.dimension.Length;

/**
 *
 */
public class Meter implements Unit {

    public static final String NAME = "Meter";
    public static final Dimension DIMENSION = Dimension.LENGTH;
    private static final Meter instance = new Meter();

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Dimension getDimension() {
        return DIMENSION;
    }

    @Override
    public double ratioToBaseUnit() {
        return 1.0;
    }

    @Override
    public Quantity fromBaseUnits(Quantity inBaseUnits) {
        return new Quantity(ratioToBaseUnit() / inBaseUnits.getValue(), this);
    }

    public static Meter getInstance() {
        return instance;
    }
}

package com.deathrayresearch.dynamics.measure.units.length;

import com.deathrayresearch.dynamics.measure.Dimension;
import com.deathrayresearch.dynamics.measure.Quantity;
import com.deathrayresearch.dynamics.measure.Unit;
import com.deathrayresearch.dynamics.measure.dimension.Length;

/**
 *
 */
public class Meter implements Unit<Length> {

    public static final String NAME = "Meter";
    public static final Dimension DIMENSION = Length.getInstance();
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
    public Quantity<Length> fromBaseUnits(Quantity<Length> inBaseUnits) {
        return new Quantity<>(ratioToBaseUnit() / inBaseUnits.getValue(), this);
    }

    public static Meter getInstance() {
        return instance;
    }
}

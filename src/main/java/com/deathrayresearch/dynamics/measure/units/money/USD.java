package com.deathrayresearch.dynamics.measure.units.money;

import com.deathrayresearch.dynamics.measure.Dimension;
import com.deathrayresearch.dynamics.measure.Unit;
import com.deathrayresearch.dynamics.measure.dimension.Money;
import com.deathrayresearch.dynamics.measure.dimension.Time;

/**
 *
 */
public class USD implements Unit<Money> {

    public static final String NAME = "USD";
    public static final Dimension DIMENSION = Time.getInstance();
    private static final USD instance = new USD();

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

    public static USD getInstance() {
        return instance;
    }
}

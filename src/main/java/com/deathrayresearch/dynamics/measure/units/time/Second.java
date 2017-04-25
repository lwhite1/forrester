package com.deathrayresearch.dynamics.measure.units.time;

import com.deathrayresearch.dynamics.measure.Dimension;
import com.deathrayresearch.dynamics.measure.Unit;
import com.deathrayresearch.dynamics.measure.dimension.Time;

import java.time.temporal.ChronoUnit;

/**
 *
 */
public class Second implements Unit<Time> {

    public static final String NAME = "Second";
    public static final ChronoUnit chronoUnit = ChronoUnit.SECONDS;

    public static final Dimension DIMENSION = Time.getInstance();
    private static final Second instance = new Second();

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

    public static Second getInstance() {
        return instance;
    }
}

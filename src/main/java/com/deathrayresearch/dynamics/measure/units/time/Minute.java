package com.deathrayresearch.dynamics.measure.units.time;

import com.deathrayresearch.dynamics.measure.Dimension;
import com.deathrayresearch.dynamics.measure.Unit;
import com.deathrayresearch.dynamics.measure.dimension.Time;

import java.time.temporal.ChronoUnit;

/**
 *
 */
public class Minute implements Unit<Time> {

    public static final String NAME = "Minute";
    public static final ChronoUnit chronoUnit = ChronoUnit.MINUTES;

    public static final Dimension DIMENSION = Time.getInstance();
    private static final Minute instance = new Minute();

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
        return 60.0;
    }

    public static Minute getInstance() {
        return instance;
    }
}

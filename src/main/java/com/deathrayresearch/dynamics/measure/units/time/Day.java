package com.deathrayresearch.dynamics.measure.units.time;

import com.deathrayresearch.dynamics.measure.Dimension;
import com.deathrayresearch.dynamics.measure.Unit;
import com.deathrayresearch.dynamics.measure.dimension.Time;

import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;

/**
 *
 */
public class Day implements Unit<Time> {

    public static final String NAME = "Day";
    public static final ChronoUnit chronoUnit = ChronoUnit.DAYS;
    public static final Dimension DIMENSION = Time.getInstance();
    private static final Day instance = new Day();

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
        return 60 * 60 * 24;
    }

    public static Day getInstance() {
        return instance;
    }


}

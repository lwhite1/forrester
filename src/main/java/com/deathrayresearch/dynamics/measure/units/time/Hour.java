package com.deathrayresearch.dynamics.measure.units.time;

import com.deathrayresearch.dynamics.measure.Dimension;
import com.deathrayresearch.dynamics.measure.Unit;
import com.deathrayresearch.dynamics.measure.dimension.Time;

import java.time.temporal.ChronoUnit;

/**
 *
 */
public class Hour implements Unit<Time> {

    public static final String NAME = "Hour";
    public static final ChronoUnit chronoUnit = ChronoUnit.HOURS;

    public static final Dimension DIMENSION = Time.getInstance();
    private static final Hour instance = new Hour();

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
        return 60 * 60;
    }

    public static Hour getInstance() {
        return instance;
    }


}

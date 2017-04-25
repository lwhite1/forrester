package com.deathrayresearch.dynamics.measure.units.time;

import com.deathrayresearch.dynamics.measure.Dimension;
import com.deathrayresearch.dynamics.measure.Unit;
import com.deathrayresearch.dynamics.measure.dimension.Time;

import java.time.temporal.ChronoUnit;

/**
 *
 */
public class Year implements Unit<Time> {

    public static final String NAME = "Year";
    public static final ChronoUnit chronoUnit = ChronoUnit.YEARS;

    public static final Dimension DIMENSION = Time.getInstance();
    private static final Year instance = new Year();

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
        return 365 * 24 * 60 * 60;
    }

    public static Year getInstance() {
        return instance;
    }


}

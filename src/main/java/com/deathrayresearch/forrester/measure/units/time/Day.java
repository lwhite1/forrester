package com.deathrayresearch.forrester.measure.units.time;


import com.deathrayresearch.forrester.measure.Dimension;
import com.deathrayresearch.forrester.measure.TimeUnit;
import com.deathrayresearch.forrester.measure.Unit;
import com.deathrayresearch.forrester.measure.dimension.Time;

import java.time.temporal.ChronoUnit;

/**
 *
 */
public class Day implements TimeUnit {

    public static final String NAME = "Day";
    public static final ChronoUnit chronoUnit = ChronoUnit.DAYS;
    public static final Dimension DIMENSION = Dimension.TIME;
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

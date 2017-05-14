package com.deathrayresearch.forrester.measure.units.time;

import com.deathrayresearch.forrester.measure.Dimension;
import com.deathrayresearch.forrester.measure.TimeUnit;
import com.deathrayresearch.forrester.measure.dimension.Time;

import java.time.temporal.ChronoUnit;

/**
 *
 */
public class Second implements TimeUnit {

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

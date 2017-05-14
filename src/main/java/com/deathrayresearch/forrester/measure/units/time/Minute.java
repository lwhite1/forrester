package com.deathrayresearch.forrester.measure.units.time;

import com.deathrayresearch.forrester.measure.Dimension;
import com.deathrayresearch.forrester.measure.TimeUnit;
import com.deathrayresearch.forrester.measure.dimension.Time;

import java.time.temporal.ChronoUnit;

/**
 *
 */
public class Minute implements TimeUnit {

    public static final String NAME = "Minute";
    public static final ChronoUnit chronoUnit = ChronoUnit.MINUTES;

    public static final Dimension DIMENSION = Dimension.TIME;
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

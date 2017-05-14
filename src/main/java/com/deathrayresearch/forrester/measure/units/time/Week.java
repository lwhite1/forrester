package com.deathrayresearch.forrester.measure.units.time;


import com.deathrayresearch.forrester.measure.Dimension;
import com.deathrayresearch.forrester.measure.TimeUnit;
import com.deathrayresearch.forrester.measure.dimension.Time;

import java.time.temporal.ChronoUnit;

/**
 *
 */
public class Week implements TimeUnit {

    public static final String NAME = "Week";
    public static final ChronoUnit chronoUnit = ChronoUnit.WEEKS;

    public static final Dimension DIMENSION = Dimension.TIME;
    private static final Week instance = new Week();

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
        return 60 * 60 * 24 * 7;
    }

    public static Week getInstance() {
        return instance;
    }


}

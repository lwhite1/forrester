package com.deathrayresearch.forrester.measure.units.time;


import com.deathrayresearch.forrester.measure.Dimension;
import com.deathrayresearch.forrester.measure.Unit;
import com.deathrayresearch.forrester.measure.dimension.Time;

import java.time.temporal.ChronoUnit;

/**
 *
 */
public class Hour implements Unit {

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

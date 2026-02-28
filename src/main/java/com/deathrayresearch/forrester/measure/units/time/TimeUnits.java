package com.deathrayresearch.forrester.measure.units.time;

import com.deathrayresearch.forrester.measure.Dimension;
import com.deathrayresearch.forrester.measure.TimeUnit;

import java.time.temporal.ChronoUnit;

/**
 * Standard units of time, from seconds to years. Each constant stores its ratio to the
 * base unit (seconds) and the corresponding {@link java.time.temporal.ChronoUnit}.
 */
public enum TimeUnits implements TimeUnit {

    SECOND("Second", 1.0, ChronoUnit.SECONDS),
    MINUTE("Minute", 60.0, ChronoUnit.MINUTES),
    HOUR("Hour", 60 * 60, ChronoUnit.HOURS),
    DAY("Day", 3600 * 24, ChronoUnit.DAYS),
    WEEK("Week", 60 * 60 * 24 * 7, ChronoUnit.WEEKS),
    YEAR("Year", 365 * 24 * 60 * 60, ChronoUnit.YEARS);

    private final String name;
    private final double ratioToBaseUnit;
    private final ChronoUnit chronoUnit;

    TimeUnits(String name, double ratioToBaseUnit, ChronoUnit chronoUnit) {
        this.name = name;
        this.ratioToBaseUnit = ratioToBaseUnit;
        this.chronoUnit = chronoUnit;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Dimension getDimension() {
        return Dimension.TIME;
    }

    @Override
    public double ratioToBaseUnit() {
        return ratioToBaseUnit;
    }

    public ChronoUnit getChronoUnit() {
        return chronoUnit;
    }
}

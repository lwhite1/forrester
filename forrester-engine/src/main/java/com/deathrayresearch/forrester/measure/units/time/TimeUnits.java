package com.deathrayresearch.forrester.measure.units.time;

import com.deathrayresearch.forrester.measure.Dimension;
import com.deathrayresearch.forrester.measure.TimeUnit;

import java.time.temporal.ChronoUnit;

/**
 * Standard units of time, from milliseconds to years. Each constant stores its ratio to the
 * base unit (seconds) and the corresponding {@link java.time.temporal.ChronoUnit}.
 *
 * <p>{@code MONTH} uses a flat 30-day approximation (2,592,000 seconds) and {@code YEAR} uses
 * a flat 365-day approximation (31,536,000 seconds). These are the standard conventions in
 * system dynamics tools (Vensim, Stella, AnyLogic) and keep step calculations exact. The
 * differences from calendar months/years are well below typical parameter uncertainty in SD
 * models.
 */
public enum TimeUnits implements TimeUnit {

    MILLISECOND("Millisecond", 0.001, ChronoUnit.MILLIS),
    SECOND("Second", 1.0, ChronoUnit.SECONDS),
    MINUTE("Minute", 60.0, ChronoUnit.MINUTES),
    HOUR("Hour", 60 * 60, ChronoUnit.HOURS),
    DAY("Day", 3600 * 24, ChronoUnit.DAYS),
    WEEK("Week", 60 * 60 * 24 * 7, ChronoUnit.WEEKS),
    MONTH("Month", 30L * 24 * 60 * 60, ChronoUnit.MONTHS),
    YEAR("Year", 365L * 24 * 60 * 60, ChronoUnit.YEARS);

    private final String name;
    private final double ratioToBaseUnit;
    private final ChronoUnit chronoUnit;

    TimeUnits(String name, double ratioToBaseUnit, ChronoUnit chronoUnit) {
        this.name = name;
        this.ratioToBaseUnit = ratioToBaseUnit;
        this.chronoUnit = chronoUnit;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return name;
    }

    /** {@inheritDoc} Returns {@link Dimension#TIME}. */
    @Override
    public Dimension getDimension() {
        return Dimension.TIME;
    }

    /** {@inheritDoc} */
    @Override
    public double ratioToBaseUnit() {
        return ratioToBaseUnit;
    }

    /**
     * Returns the {@link ChronoUnit} that corresponds to this time unit, useful for
     * interoperating with the {@code java.time} API.
     *
     * @return the corresponding {@link ChronoUnit}
     */
    public ChronoUnit getChronoUnit() {
        return chronoUnit;
    }
}

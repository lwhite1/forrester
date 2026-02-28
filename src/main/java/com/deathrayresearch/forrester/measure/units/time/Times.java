package com.deathrayresearch.forrester.measure.units.time;

import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.TimeUnit;

/**
 * Factory methods for creating time {@link Quantity} instances in various time units.
 */
public class Times {

    /**
     * Creates a quantity of the given value in seconds.
     */
    public static Quantity seconds(double value) {
        TimeUnit timeUnit = TimeUnits.SECOND;
        return new Quantity(value, timeUnit);
    }

    /**
     * Creates a quantity of the given value in minutes.
     */
    public static Quantity minutes(double value) {
        TimeUnit timeUnit = TimeUnits.MINUTE;
        return new Quantity(value, timeUnit);
    }

    /**
     * Creates a quantity of the given value in hours.
     */
    public static Quantity hours(double value) {
        TimeUnit timeUnit = TimeUnits.HOUR;
        return new Quantity(value, timeUnit);
    }

    /**
     * Creates a quantity of the given value in days.
     */
    public static Quantity days(double value) {
        TimeUnit timeUnit = TimeUnits.DAY;
        return new Quantity(value, timeUnit);
    }

    /**
     * Creates a quantity of the given value in weeks.
     */
    public static Quantity weeks(double value) {
        TimeUnit timeUnit = TimeUnits.WEEK;
        return new Quantity(value, timeUnit);
    }

    /**
     * Creates a quantity of the given value in years.
     */
    public static Quantity years(double value) {
        TimeUnit timeUnit = TimeUnits.YEAR;
        return new Quantity(value, timeUnit);
    }
}

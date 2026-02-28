package com.deathrayresearch.forrester.measure.units.time;

import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.TimeUnit;

public class Times {

    public static Quantity seconds(double value) {
        TimeUnit timeUnit = TimeUnits.SECOND;
        return new Quantity(value, timeUnit);
    }

    public static Quantity minutes(double value) {
        TimeUnit timeUnit = TimeUnits.MINUTE;
        return new Quantity(value, timeUnit);
    }

    public static Quantity hours(double value) {
        TimeUnit timeUnit = TimeUnits.HOUR;
        return new Quantity(value, timeUnit);
    }

    public static Quantity days(double value) {
        TimeUnit timeUnit = TimeUnits.DAY;
        return new Quantity(value, timeUnit);
    }

    public static Quantity weeks(double value) {
        TimeUnit timeUnit = TimeUnits.WEEK;
        return new Quantity(value, timeUnit);
    }

    public static Quantity years(double value) {
        TimeUnit timeUnit = TimeUnits.YEAR;
        return new Quantity(value, timeUnit);
    }
}

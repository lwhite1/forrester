package com.deathrayresearch.forrester.measure.units.time;


import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.TimeUnit;

/**
 *
 */
public class Times {

    public static Quantity seconds(double value) {
        TimeUnit timeUnit = Second.getInstance();
        return new Quantity(value, timeUnit);
    }

    public static Quantity minutes(double value) {
        TimeUnit timeUnit = Minute.getInstance();
        return new Quantity(value, timeUnit);
    }

    public static Quantity hours(double value) {
        TimeUnit timeUnit = Hour.getInstance();
        return new Quantity(value, timeUnit);
    }

    public static Quantity days(double value) {
        TimeUnit timeUnit = Day.getInstance();
        return new Quantity(value, timeUnit);
    }

    public static Quantity weeks(double value) {
        TimeUnit timeUnit = Week.getInstance();
        return new Quantity(value, timeUnit);
    }

    public static Quantity years(double value) {
        TimeUnit timeUnit = Year.getInstance();
        return new Quantity(value, timeUnit);
    }
}

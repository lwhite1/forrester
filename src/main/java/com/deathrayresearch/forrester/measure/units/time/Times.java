package com.deathrayresearch.forrester.measure.units.time;


import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.TimeUnit;

/**
 *
 */
public class Times {

    public static Second SECOND = Second.getInstance();
    public static Minute MINUTE = Minute.getInstance();
    public static Hour HOUR = Hour.getInstance();
    public static Day DAY = Day.getInstance();
    public static Week WEEK = Week.getInstance();
    public static Year YEAR = Year.getInstance();

    public static Quantity seconds(String name, double value) {
        TimeUnit timeUnit = Second.getInstance();
        return new Quantity(name, value, timeUnit);
    }

    public static Quantity minutes(String name, double value) {
        TimeUnit timeUnit = Minute.getInstance();
        return new Quantity(name, value, timeUnit);
    }

    public static Quantity hours(String name, double value) {
        TimeUnit timeUnit = Hour.getInstance();
        return new Quantity(name, value, timeUnit);
    }

    public static Quantity days(String name, double value) {
        TimeUnit timeUnit = Day.getInstance();
        return new Quantity(name, value, timeUnit);
    }

    public static Quantity weeks(String name, double value) {
        TimeUnit timeUnit = Week.getInstance();
        return new Quantity(name, value, timeUnit);
    }

    public static Quantity years(String name, double value) {
        TimeUnit timeUnit = Year.getInstance();
        return new Quantity(name, value, timeUnit);
    }
}

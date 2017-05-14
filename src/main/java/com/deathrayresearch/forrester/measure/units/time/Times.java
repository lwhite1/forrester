package com.deathrayresearch.forrester.measure.units.time;


import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.Unit;

/**
 *
 */
public class Times {

    public static Quantity seconds(double value) {
        Unit timeUnit = Second.getInstance();
        return new Quantity(value, timeUnit);
    }

    public static Quantity minutes(double value) {
        Unit timeUnit = Minute.getInstance();
        return new Quantity(value, timeUnit);
    }

    public static Quantity hours(double value) {
        Unit timeUnit = Hour.getInstance();
        return new Quantity(value, timeUnit);
    }

    public static Quantity days(double value) {
        Unit timeUnit = Day.getInstance();
        return new Quantity(value, timeUnit);
    }

    public static Quantity weeks(double value) {
        Unit timeUnit = Week.getInstance();
        return new Quantity(value, timeUnit);
    }

    public static Quantity years(double value) {
        Unit timeUnit = Year.getInstance();
        return new Quantity(value, timeUnit);
    }
}

package com.deathrayresearch.dynamics.measure.units.time;

import com.deathrayresearch.dynamics.measure.Quantity;
import com.deathrayresearch.dynamics.measure.Unit;
import com.deathrayresearch.dynamics.measure.dimension.Time;

/**
 *
 */
public class Times {

    public static Quantity<Time> seconds(double value) {
        Unit<Time> timeUnit = Second.getInstance();
        return new Quantity<>(value, timeUnit);
    }

    public static Quantity<Time> minutes(double value) {
        Unit<Time> timeUnit = Minute.getInstance();
        return new Quantity<>(value, timeUnit);
    }

    public static Quantity<Time> hours(double value) {
        Unit<Time> timeUnit = Hour.getInstance();
        return new Quantity<>(value, timeUnit);
    }

    public static Quantity<Time> days(double value) {
        Unit<Time> timeUnit = Day.getInstance();
        return new Quantity<>(value, timeUnit);
    }

    public static Quantity<Time> weeks(double value) {
        Unit<Time> timeUnit = Week.getInstance();
        return new Quantity<>(value, timeUnit);
    }

    public static Quantity<Time> years(double value) {
        Unit<Time> timeUnit = Year.getInstance();
        return new Quantity<>(value, timeUnit);
    }
}

package systems.courant.sd.measure.units.time;

import systems.courant.sd.measure.Quantity;
import systems.courant.sd.measure.TimeUnit;

/**
 * Factory methods for creating time {@link Quantity} instances in various time units.
 */
public final class Times {

    private Times() {
    }

    /**
     * Creates a quantity of the given value in milliseconds.
     */
    public static Quantity milliseconds(double value) {
        return new Quantity(value, TimeUnits.MILLISECOND);
    }

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
     * Creates a quantity of the given value in months (30-day approximation).
     */
    public static Quantity months(double value) {
        return new Quantity(value, TimeUnits.MONTH);
    }

    /**
     * Creates a quantity of the given value in years.
     */
    public static Quantity years(double value) {
        TimeUnit timeUnit = TimeUnits.YEAR;
        return new Quantity(value, timeUnit);
    }
}

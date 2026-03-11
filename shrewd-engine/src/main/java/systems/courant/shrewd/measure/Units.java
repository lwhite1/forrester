package systems.courant.shrewd.measure;

import systems.courant.shrewd.measure.units.dimensionless.DimensionlessUnits;
import systems.courant.shrewd.measure.units.item.ItemUnits;
import systems.courant.shrewd.measure.units.length.LengthUnits;
import systems.courant.shrewd.measure.units.mass.MassUnits;
import systems.courant.shrewd.measure.units.money.MoneyUnits;
import systems.courant.shrewd.measure.units.temperature.TemperatureUnits;
import systems.courant.shrewd.measure.units.time.TimeUnits;
import systems.courant.shrewd.measure.units.volume.VolumeUnits;

/**
 * Central registry of commonly used unit constants across all dimensions.
 * Provides convenient static access to units of time, length, mass, volume, money,
 * temperature, and items.
 */
public class Units {

    // dimensionless
    public static final Unit DIMENSIONLESS = DimensionlessUnits.DIMENSIONLESS;

    // items
    public static final Unit PEOPLE = ItemUnits.PEOPLE;
    public static final Unit THING = ItemUnits.THING;

    // length
    public static final Unit MILLIMETER = LengthUnits.MILLIMETER;
    public static final Unit CENTIMETER = LengthUnits.CENTIMETER;
    public static final Unit METER = LengthUnits.METER;
    public static final Unit KILOMETER = LengthUnits.KILOMETER;
    public static final Unit INCH = LengthUnits.INCH;
    public static final Unit FOOT = LengthUnits.FOOT;
    public static final Unit YARD = LengthUnits.YARD;
    public static final Unit MILE = LengthUnits.MILE;
    public static final Unit NAUTICAL_MILE = LengthUnits.NAUTICAL_MILE;

    // mass
    public static final Unit MILLIGRAM = MassUnits.MILLIGRAM;
    public static final Unit GRAM = MassUnits.GRAM;
    public static final Unit KILOGRAM = MassUnits.KILOGRAM;
    public static final Unit METRIC_TON = MassUnits.METRIC_TON;
    public static final Unit OUNCE = MassUnits.OUNCE;
    public static final Unit POUND = MassUnits.POUND;
    public static final Unit SHORT_TON = MassUnits.SHORT_TON;

    // money
    public static final Unit US_DOLLAR = MoneyUnits.USD;

    // time
    public static final TimeUnit MILLISECOND = TimeUnits.MILLISECOND;
    public static final TimeUnit SECOND = TimeUnits.SECOND;
    public static final TimeUnit MINUTE = TimeUnits.MINUTE;
    public static final TimeUnit HOUR = TimeUnits.HOUR;
    public static final TimeUnit DAY = TimeUnits.DAY;
    public static final TimeUnit WEEK = TimeUnits.WEEK;
    public static final TimeUnit MONTH = TimeUnits.MONTH;
    public static final TimeUnit YEAR = TimeUnits.YEAR;

    // volume
    public static final Unit MILLILITER = VolumeUnits.MILLILITER;
    public static final Unit LITER = VolumeUnits.LITER;
    public static final Unit CUBIC_METER = VolumeUnits.CUBIC_METER;
    public static final Unit FLUID_OUNCE_US = VolumeUnits.FLUID_OUNCE_US;
    public static final Unit CUP_US = VolumeUnits.CUP_US;
    public static final Unit PINT_US = VolumeUnits.PINT_US;
    public static final Unit QUART_US = VolumeUnits.QUART_US;
    public static final Unit GALLON_US = VolumeUnits.GALLON_US;
    public static final Unit IMPERIAL_GALLON = VolumeUnits.IMPERIAL_GALLON;
    public static final Unit BARREL = VolumeUnits.BARREL;

    // temperature
    public static final Unit CELSIUS = TemperatureUnits.CELSIUS;
    public static final Unit FAHRENHEIT = TemperatureUnits.FAHRENHEIT;
}

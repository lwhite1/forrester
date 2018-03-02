package com.deathrayresearch.forrester.measure;

import com.deathrayresearch.forrester.measure.units.dimensionless.DimensionlessUnit;
import com.deathrayresearch.forrester.measure.units.item.People;
import com.deathrayresearch.forrester.measure.units.item.Thing;
import com.deathrayresearch.forrester.measure.units.length.Foot;
import com.deathrayresearch.forrester.measure.units.length.Inch;
import com.deathrayresearch.forrester.measure.units.length.Meter;
import com.deathrayresearch.forrester.measure.units.length.Mile;
import com.deathrayresearch.forrester.measure.units.length.NauticalMile;
import com.deathrayresearch.forrester.measure.units.mass.Kilogram;
import com.deathrayresearch.forrester.measure.units.mass.Ounce;
import com.deathrayresearch.forrester.measure.units.mass.Pound;
import com.deathrayresearch.forrester.measure.units.money.USD;
import com.deathrayresearch.forrester.measure.units.temperature.Centigrade;
import com.deathrayresearch.forrester.measure.units.time.Day;
import com.deathrayresearch.forrester.measure.units.time.Hour;
import com.deathrayresearch.forrester.measure.units.time.Minute;
import com.deathrayresearch.forrester.measure.units.time.Second;
import com.deathrayresearch.forrester.measure.units.time.Week;
import com.deathrayresearch.forrester.measure.units.time.Year;
import com.deathrayresearch.forrester.measure.units.volume.CubicMeter;
import com.deathrayresearch.forrester.measure.units.volume.FluidOunceUS;
import com.deathrayresearch.forrester.measure.units.volume.GallonUS;
import com.deathrayresearch.forrester.measure.units.volume.Liter;
import com.deathrayresearch.forrester.measure.units.volume.QuartUS;

public class Units {

    // dimensionless
    public static final DimensionlessUnit DIMENSIONLESS = DimensionlessUnit.getInstance();

    // items
    public static final People PEOPLE = People.getInstance();
    public static final Thing THING = Thing.getInstance();

    // length
    public static final Foot FOOT = Foot.getInstance();
    public static final Inch INCH = Inch.getInstance();
    public static final Meter METER = Meter.getInstance();
    public static final Mile MILE = Mile.getInstance();
    public static final NauticalMile NAUTICAL_MILE = NauticalMile.getInstance();

    // mass
    public static final Kilogram KILOGRAM = Kilogram.getInstance();
    public static final Ounce OUNCE = Ounce.getInstance();
    public static final Pound POUND = Pound.getInstance();

    // money
    public static final USD US_DOLLAR = USD.getInstance();

    // time
    public static final Day DAY = Day.getInstance();
    public static final Hour HOUR = Hour.getInstance();
    public static final Minute MINUTE = Minute.getInstance();
    public static final Second SECOND = Second.getInstance();
    public static final Week WEEK = Week.getInstance();
    public static final Year YEAR = Year.getInstance();

    // volume
    public static final CubicMeter CUBIC_METER = CubicMeter.getInstance();
    public static final Liter LITER = Liter.getInstance();

    public static final FluidOunceUS FLUID_OUNCE_US = FluidOunceUS.getInstance();
    public static final GallonUS GALLON_US = GallonUS.getInstance();
    public static final QuartUS QUART_US = QuartUS.getInstance();

    // temperature
    public static final Centigrade CENTIGRADE = Centigrade.getInstance();


}

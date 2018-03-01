package com.deathrayresearch.forrester.measure;

import com.deathrayresearch.forrester.measure.units.dimensionless.DimensionlessUnit;
import com.deathrayresearch.forrester.measure.units.item.People;
import com.deathrayresearch.forrester.measure.units.item.Thing;
import com.deathrayresearch.forrester.measure.units.length.Foot;
import com.deathrayresearch.forrester.measure.units.length.Inch;
import com.deathrayresearch.forrester.measure.units.length.Meter;
import com.deathrayresearch.forrester.measure.units.length.Mile;
import com.deathrayresearch.forrester.measure.units.length.NauticalMile;

public class Units {

    public static final DimensionlessUnit DIMENSIONLESS = DimensionlessUnit.getInstance();

    public static final People PEOPLE = People.getInstance();
    public static final Thing THING = Thing.getInstance();

    public static final Foot FOOT = Foot.getInstance();
    public static final Inch INCH = Inch.getInstance();
    public static final Meter METER = Meter.getInstance();
    public static final Mile MILE = Mile.getInstance();
    public static final NauticalMile NAUTICAL_MILE = NauticalMile.getInstance();
}

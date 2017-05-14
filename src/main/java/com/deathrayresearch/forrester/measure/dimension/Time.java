package com.deathrayresearch.forrester.measure.dimension;

import com.deathrayresearch.forrester.measure.Converter;
import com.deathrayresearch.forrester.measure.Dimension;
import com.deathrayresearch.forrester.measure.Unit;
import com.deathrayresearch.forrester.measure.units.time.Second;

/**
 *
 */
public class Time implements Dimension {

    private static Time ourInstance = new Time();

    public static Time getInstance() {
        return ourInstance;
    }

    private Time() {}

    @Override
    public Unit getBaseUnit() {
        return Second.getInstance();
    }

    @Override
    public Converter getConverter() {
        return null;
    }
}

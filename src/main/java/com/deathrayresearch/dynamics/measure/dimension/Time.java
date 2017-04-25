package com.deathrayresearch.dynamics.measure.dimension;

import com.deathrayresearch.dynamics.measure.Converter;
import com.deathrayresearch.dynamics.measure.Dimension;
import com.deathrayresearch.dynamics.measure.Unit;
import com.deathrayresearch.dynamics.measure.units.time.Second;

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

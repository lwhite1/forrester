package com.deathrayresearch.dynamics.measure.dimension;

import com.deathrayresearch.dynamics.measure.Converter;
import com.deathrayresearch.dynamics.measure.Dimension;
import com.deathrayresearch.dynamics.measure.Unit;
import com.deathrayresearch.dynamics.measure.units.money.USD;

/**
 *
 */
public class Money implements Dimension {

    private static Money ourInstance = new Money();

    public static Money getInstance() {
        return ourInstance;
    }

    @Override
    public Converter getConverter() {
        return null;
    }

    @Override
    public Unit getBaseUnit() {
        return USD.getInstance();
    }
}

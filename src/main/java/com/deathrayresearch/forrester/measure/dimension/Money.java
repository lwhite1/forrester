package com.deathrayresearch.forrester.measure.dimension;

import com.deathrayresearch.forrester.measure.Converter;
import com.deathrayresearch.forrester.measure.Dimension;
import com.deathrayresearch.forrester.measure.Unit;
import com.deathrayresearch.forrester.measure.units.money.USD;

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

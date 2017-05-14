package com.deathrayresearch.forrester.measure;

/**
 *
 */
public interface Dimension {

    Converter getConverter();

    Unit getBaseUnit();
}

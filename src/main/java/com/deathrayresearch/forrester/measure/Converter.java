package com.deathrayresearch.forrester.measure;

/**
 *
 */
public interface Converter{

    Quantity convert(Quantity originalQuantity, Unit newUnit);
}

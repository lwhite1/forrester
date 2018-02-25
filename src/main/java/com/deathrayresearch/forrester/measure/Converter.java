package com.deathrayresearch.forrester.measure;

/**
 * Converts quantities from their current unit, to the newUnit, assuming the conversion is legitimate (i.e.,
 * we're not trying to convert inches to pounds)
 */
public interface Converter{

    Quantity convert(Quantity originalQuantity, Unit newUnit);
}

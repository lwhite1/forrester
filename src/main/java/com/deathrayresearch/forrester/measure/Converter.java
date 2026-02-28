package com.deathrayresearch.forrester.measure;

/**
 * Converts quantities from their current unit, to the newUnit, assuming the conversion is legitimate (i.e.,
 * we're not trying to convert inches to pounds)
 */
public interface Converter{

    /**
     * Converts the given quantity to the specified unit.
     *
     * @param originalQuantity the quantity to convert
     * @param newUnit          the target unit (must be in the same dimension)
     * @return the converted quantity
     */
    Quantity convert(Quantity originalQuantity, Unit newUnit);
}

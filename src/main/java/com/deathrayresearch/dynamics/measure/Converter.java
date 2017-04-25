package com.deathrayresearch.dynamics.measure;

/**
 *
 */
public interface Converter <E extends Dimension>{

    Quantity convert(Quantity<E> originalQuantity, Unit<E> newUnit);
}

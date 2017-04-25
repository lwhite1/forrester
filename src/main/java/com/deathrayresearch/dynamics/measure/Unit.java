package com.deathrayresearch.dynamics.measure;

/**
 *
 */
public interface Unit<E extends Dimension> {

    String getName();

    Dimension getDimension();

    double ratioToBaseUnit();

    default Unit<E> getBaseUnit() {
        return getDimension().getBaseUnit();
    }

    default Quantity<E> fromBaseUnits(Quantity<E> inBaseUnits) {
        double ratioOfBaseUnitsToThisUnit = ratioToBaseUnit();
        double numberOfBaseUnitsInParameter = inBaseUnits.getValue();
        return new Quantity<>(numberOfBaseUnitsInParameter * ratioOfBaseUnitsToThisUnit, this);
    }
}

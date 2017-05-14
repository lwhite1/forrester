package com.deathrayresearch.forrester.measure;

/**
 *
 */
public interface Unit {

    String getName();

    Dimension getDimension();

    double ratioToBaseUnit();

    default Unit getBaseUnit() {
        return getDimension().getBaseUnit();
    }

    default Quantity fromBaseUnits(Quantity inBaseUnits) {
        double ratioOfBaseUnitsToThisUnit = ratioToBaseUnit();
        double numberOfBaseUnitsInParameter = inBaseUnits.getValue();
        return new Quantity(numberOfBaseUnitsInParameter * ratioOfBaseUnitsToThisUnit, this);
    }
}

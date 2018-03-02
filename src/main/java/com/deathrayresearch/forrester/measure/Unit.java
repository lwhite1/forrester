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
        double numberOfCurrentUnitsInArgument = inBaseUnits.getValue() / ratioToBaseUnit();
        return new Quantity(numberOfCurrentUnitsInArgument, this);
    }

    default double fromBaseUnits(double inBaseUnits) {
        return inBaseUnits / ratioToBaseUnit();
    }

    default Quantity toBaseUnits(Quantity quantity) {
        return new Quantity(quantity.getValue() * ratioToBaseUnit(), getBaseUnit());
    }

    default double toBaseUnits(double amount) {
        return amount * ratioToBaseUnit();
    }
}

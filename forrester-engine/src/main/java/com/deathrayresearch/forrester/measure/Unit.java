package com.deathrayresearch.forrester.measure;

/**
 * A unit of measure within a particular {@link Dimension} (e.g., meters in the length dimension).
 * Every unit defines a ratio to its dimension's base unit, enabling automatic conversion between
 * compatible units.
 */
public interface Unit {

    /**
     * Returns the human-readable name of this unit (e.g., "Meter", "Kilogram", "Day").
     *
     * @return the unit name
     */
    String getName();

    /**
     * Returns the dimension this unit belongs to (e.g., TIME, MASS, LENGTH).
     *
     * @return the dimension
     */
    Dimension getDimension();

    /**
     * Returns the multiplicative factor that converts one of this unit to the dimension's base unit.
     * For example, if the base unit is meters and this unit is kilometers, the ratio is 1000.0.
     *
     * @return the ratio of this unit to the base unit
     */
    double ratioToBaseUnit();

    /**
     * Returns the base unit for this unit's dimension.
     */
    default Unit getBaseUnit() {
        return getDimension().getBaseUnit();
    }

    /**
     * Converts a quantity expressed in base units to this unit.
     *
     * @param inBaseUnits the quantity in base units
     * @return an equivalent quantity expressed in this unit
     */
    default Quantity fromBaseUnits(Quantity inBaseUnits) {
        double numberOfCurrentUnitsInArgument = inBaseUnits.getValue() / ratioToBaseUnit();
        return new Quantity(numberOfCurrentUnitsInArgument, this);
    }

    /**
     * Converts a raw value expressed in base units to the equivalent value in this unit.
     *
     * @param inBaseUnits the value in base units
     * @return the equivalent value in this unit
     */
    default double fromBaseUnits(double inBaseUnits) {
        return inBaseUnits / ratioToBaseUnit();
    }

    /**
     * Converts a quantity expressed in this unit to the dimension's base unit.
     *
     * @param quantity the quantity in this unit
     * @return an equivalent quantity expressed in the base unit
     */
    default Quantity toBaseUnits(Quantity quantity) {
        return new Quantity(quantity.getValue() * ratioToBaseUnit(), getBaseUnit());
    }

    /**
     * Converts a raw value expressed in this unit to the equivalent value in the base unit.
     *
     * @param amount the value in this unit
     * @return the equivalent value in the base unit
     */
    default double toBaseUnits(double amount) {
        return amount * ratioToBaseUnit();
    }
}

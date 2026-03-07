package com.deathrayresearch.forrester.model.flows;

import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.TimeUnit;
import com.google.common.base.Preconditions;

/**
 * Converts quantities associated with a rate from one time unit to another
 */
public final class RateConverter {

    private RateConverter() {
        // prevent instantiation
    }

    /**
     * Converts a rate quantity from one time unit to another.
     *
     * <p>Assumes the quantity is produced per one unit of {@code originalTimeUnit} and computes
     * how much would be produced per one unit of {@code newTimeUnit}. For example, 700 per week
     * converts to 100 per day.
     *
     * @param originalQuantity the quantity per original time unit
     * @param originalTimeUnit the time unit the quantity is expressed in
     * @param newTimeUnit      the target time unit
     * @return a new Quantity with the converted value and the original quantity's unit
     */
    public static Quantity convert(Quantity originalQuantity, TimeUnit originalTimeUnit, TimeUnit newTimeUnit) {
        Preconditions.checkArgument(originalQuantity != null, "originalQuantity cannot be null");
        Preconditions.checkArgument(originalTimeUnit != null, "originalTimeUnit cannot be null");
        Preconditions.checkArgument(newTimeUnit != null, "newTimeUnit cannot be null");

        double convertedValue =
            originalQuantity.getValue() * newTimeUnit.ratioToBaseUnit() / originalTimeUnit.ratioToBaseUnit();

        return new Quantity(convertedValue, originalQuantity.getUnit());
    }
}

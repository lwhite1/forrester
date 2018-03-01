package com.deathrayresearch.forrester.rate;

import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.TimeUnit;
import com.google.common.base.Preconditions;

/**
 * Converts quantities associated with a rate from one time unit to another
 */
class RateConverter {

    /**
     * Returns a new Quantity from the original quantity.
     * It is assumed that the quantity is produced in one unit of originalTimeUnit, and the result is how much
     * would have been produced in a single unit of the newTimeUnit.
     *
     * For example, if the originalQuantity was 700, the originalTimeUnit was a week, and the newTimeUnit a day,
     * the result would be 100
     */
    static Quantity convert(Quantity originalQuantity, TimeUnit originalTimeUnit, TimeUnit newTimeUnit) {
        Preconditions.checkArgument(originalQuantity != null, "originalQuantity cannot be null");
        Preconditions.checkArgument(originalTimeUnit != null, "originalTimeUnit cannot be null");
        Preconditions.checkArgument(newTimeUnit != null, "newTimeUnit cannot be null");

        double inBaseUnits = originalTimeUnit.toBaseUnits(originalQuantity.getValue());

        double convertedValue =
            inBaseUnits == 0 ? 0 : (newTimeUnit.ratioToBaseUnit() / inBaseUnits) * originalQuantity.getValue();

        return new Quantity(originalQuantity.getName(), convertedValue * originalQuantity.getValue(), originalQuantity.getUnit());
    }
}

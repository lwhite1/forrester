package com.deathrayresearch.forrester.rate;

import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.TimeUnit;

/**
 *
 */
public class RateConverter {

    public static Quantity convert(Quantity original, TimeUnit originalTimeUnit, TimeUnit newTimeUnit) {
        double inBaseUnits = originalTimeUnit.toBaseUnits(original.getValue());
        double convertedValue = (newTimeUnit.ratioToBaseUnit() / inBaseUnits) * original.getValue();
        return new Quantity(convertedValue * original.getValue(), original.getUnit());
    }


}

package com.deathrayresearch.forrester.measure.dimension;

import com.deathrayresearch.forrester.measure.Dimension;
import com.deathrayresearch.forrester.measure.Unit;
import com.deathrayresearch.forrester.measure.units.money.MoneyUnits;

/**
 * The money (currency) dimension. Base unit is US dollars.
 */
public enum Money implements Dimension {

    INSTANCE;

    /** {@inheritDoc} Returns {@link MoneyUnits#USD}. */
    @Override
    public Unit getBaseUnit() {
        return MoneyUnits.USD;
    }
}

package com.deathrayresearch.forrester.measure.dimension;

import com.deathrayresearch.forrester.measure.Dimension;
import com.deathrayresearch.forrester.measure.Unit;
import com.deathrayresearch.forrester.measure.units.money.MoneyUnits;

public enum Money implements Dimension {

    INSTANCE;

    public static Money getInstance() {
        return INSTANCE;
    }

    @Override
    public Unit getBaseUnit() {
        return MoneyUnits.USD;
    }
}

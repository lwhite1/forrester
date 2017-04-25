package com.deathrayresearch.dynamics.rate;

import com.deathrayresearch.dynamics.measure.Dimension;
import com.deathrayresearch.dynamics.measure.Quantity;
import com.deathrayresearch.dynamics.measure.Unit;
import com.deathrayresearch.dynamics.measure.dimension.Time;

/**
 *
 */
public class FixedRate<Q extends Dimension> implements Rate<Q> {

    private Quantity<Q> quantity;
    private Unit<Time> timeUnit;

    public FixedRate(Quantity<Q> quantity, Unit<Time> timeUnit) {
        this.quantity = quantity;
        this.timeUnit = timeUnit;
    }

    public Quantity<Q> getQuantity() {
        return quantity;
    }

    public Unit<Time> getTimeUnit() {
        return timeUnit;
    }

    @Override
    public Quantity<Q> flowPerTimeUnit(Unit<Time> timeUnit) {
        double newUnitInBaseUnits = timeUnit.ratioToBaseUnit();
        double valueInBaseUnits = this.getTimeUnit().ratioToBaseUnit();
        double resultInBaseUnits = newUnitInBaseUnits / valueInBaseUnits;
        return quantity.multiply(resultInBaseUnits);
    }

    @Override
    public String toString() {
        return ""
                + quantity
                + " per "
                + timeUnit.getName();
    }
}

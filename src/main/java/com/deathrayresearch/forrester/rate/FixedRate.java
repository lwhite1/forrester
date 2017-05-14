package com.deathrayresearch.forrester.rate;


import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.Unit;

/**
 *
 */
public class FixedRate implements Rate {

    private Quantity quantity;
    private Unit timeUnit;

    public FixedRate(Quantity quantity, Unit timeUnit) {
        this.quantity = quantity;
        this.timeUnit = timeUnit;
    }

    public Quantity getQuantity() {
        return quantity;
    }

    public Unit getTimeUnit() {
        return timeUnit;
    }

    @Override
    public Quantity flowPerTimeUnit(Unit timeUnit) {
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

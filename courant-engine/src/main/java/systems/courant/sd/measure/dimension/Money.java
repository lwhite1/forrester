package systems.courant.sd.measure.dimension;

import systems.courant.sd.measure.Dimension;
import systems.courant.sd.measure.Unit;
import systems.courant.sd.measure.units.money.MoneyUnits;

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

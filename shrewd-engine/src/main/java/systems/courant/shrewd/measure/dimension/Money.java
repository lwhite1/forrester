package systems.courant.shrewd.measure.dimension;

import systems.courant.shrewd.measure.Dimension;
import systems.courant.shrewd.measure.Unit;
import systems.courant.shrewd.measure.units.money.MoneyUnits;

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

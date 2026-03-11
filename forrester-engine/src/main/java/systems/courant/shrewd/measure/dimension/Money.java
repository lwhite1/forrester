package systems.courant.forrester.measure.dimension;

import systems.courant.forrester.measure.Dimension;
import systems.courant.forrester.measure.Unit;
import systems.courant.forrester.measure.units.money.MoneyUnits;

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

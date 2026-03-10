package systems.courant.forrester.measure.units.money;

import systems.courant.forrester.measure.Dimension;
import systems.courant.forrester.measure.Unit;

/**
 * Units of currency. Currently only US dollars are defined.
 */
public enum MoneyUnits implements Unit {

    USD("USD", 1.0);

    private final String name;
    private final double ratioToBaseUnit;

    MoneyUnits(String name, double ratioToBaseUnit) {
        this.name = name;
        this.ratioToBaseUnit = ratioToBaseUnit;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return name;
    }

    /** {@inheritDoc} Returns {@link Dimension#MONEY}. */
    @Override
    public Dimension getDimension() {
        return Dimension.MONEY;
    }

    /** {@inheritDoc} */
    @Override
    public double ratioToBaseUnit() {
        return ratioToBaseUnit;
    }
}

package systems.courant.forrester.measure.units.item;

import systems.courant.forrester.measure.Dimension;
import systems.courant.forrester.measure.Unit;

/**
 * Built-in units for countable items such as generic things and people.
 */
public enum ItemUnits implements Unit {

    THING("Thing", 1.0),
    PEOPLE("Person", 1.0);

    private final String name;
    private final double ratioToBaseUnit;

    ItemUnits(String name, double ratioToBaseUnit) {
        this.name = name;
        this.ratioToBaseUnit = ratioToBaseUnit;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return name;
    }

    /** {@inheritDoc} Returns {@link Dimension#ITEM}. */
    @Override
    public Dimension getDimension() {
        return Dimension.ITEM;
    }

    /** {@inheritDoc} */
    @Override
    public double ratioToBaseUnit() {
        return ratioToBaseUnit;
    }
}

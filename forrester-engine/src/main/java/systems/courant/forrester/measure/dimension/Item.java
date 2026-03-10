package systems.courant.forrester.measure.dimension;

import systems.courant.forrester.measure.Dimension;
import systems.courant.forrester.measure.Unit;
import systems.courant.forrester.measure.units.item.ItemUnits;

/**
 * The item (countable entity) dimension. Base unit is "thing".
 */
public enum Item implements Dimension {

    INSTANCE;

    /** {@inheritDoc} Returns {@link ItemUnits#THING}. */
    @Override
    public Unit getBaseUnit() {
        return ItemUnits.THING;
    }
}

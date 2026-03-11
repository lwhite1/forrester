package systems.courant.shrewd.measure.dimension;

import systems.courant.shrewd.measure.Dimension;
import systems.courant.shrewd.measure.Unit;
import systems.courant.shrewd.measure.units.item.ItemUnits;

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

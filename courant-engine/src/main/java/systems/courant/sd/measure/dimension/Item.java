package systems.courant.sd.measure.dimension;

import systems.courant.sd.measure.Dimension;
import systems.courant.sd.measure.Unit;
import systems.courant.sd.measure.units.item.ItemUnits;

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

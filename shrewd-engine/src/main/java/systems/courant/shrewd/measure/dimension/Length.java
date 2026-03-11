package systems.courant.shrewd.measure.dimension;

import systems.courant.shrewd.measure.Dimension;
import systems.courant.shrewd.measure.Unit;
import systems.courant.shrewd.measure.units.length.LengthUnits;

/**
 * The length dimension. Base unit is meters.
 */
public enum Length implements Dimension {

    INSTANCE;

    /** {@inheritDoc} Returns {@link LengthUnits#METER}. */
    @Override
    public Unit getBaseUnit() {
        return LengthUnits.METER;
    }
}

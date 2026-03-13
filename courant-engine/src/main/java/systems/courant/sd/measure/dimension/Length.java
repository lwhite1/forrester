package systems.courant.sd.measure.dimension;

import systems.courant.sd.measure.Dimension;
import systems.courant.sd.measure.Unit;
import systems.courant.sd.measure.units.length.LengthUnits;

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

package systems.courant.sd.measure.dimension;

import systems.courant.sd.measure.Dimension;
import systems.courant.sd.measure.Unit;
import systems.courant.sd.measure.units.area.AreaUnits;

/**
 * The area dimension. Base unit is square meters.
 */
public enum Area implements Dimension {

    INSTANCE;

    /** {@inheritDoc} Returns {@link AreaUnits#SQUARE_METER}. */
    @Override
    public Unit getBaseUnit() {
        return AreaUnits.SQUARE_METER;
    }
}

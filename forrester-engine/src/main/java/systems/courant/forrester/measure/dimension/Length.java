package systems.courant.forrester.measure.dimension;

import systems.courant.forrester.measure.Dimension;
import systems.courant.forrester.measure.Unit;
import systems.courant.forrester.measure.units.length.LengthUnits;

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

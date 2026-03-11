package systems.courant.shrewd.measure.dimension;

import systems.courant.shrewd.measure.Dimension;
import systems.courant.shrewd.measure.Unit;
import systems.courant.shrewd.measure.units.time.TimeUnits;

/**
 * The time dimension. Base unit is seconds.
 */
public enum Time implements Dimension {

    INSTANCE;

    /** {@inheritDoc} Returns {@link TimeUnits#SECOND}. */
    @Override
    public Unit getBaseUnit() {
        return TimeUnits.SECOND;
    }
}

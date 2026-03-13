package systems.courant.sd.measure.dimension;

import systems.courant.sd.measure.Dimension;
import systems.courant.sd.measure.Unit;
import systems.courant.sd.measure.units.time.TimeUnits;

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

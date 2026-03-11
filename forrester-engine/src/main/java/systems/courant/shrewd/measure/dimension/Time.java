package systems.courant.forrester.measure.dimension;

import systems.courant.forrester.measure.Dimension;
import systems.courant.forrester.measure.Unit;
import systems.courant.forrester.measure.units.time.TimeUnits;

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

package systems.courant.forrester.measure.dimension;

import systems.courant.forrester.measure.Dimension;
import systems.courant.forrester.measure.Unit;
import systems.courant.forrester.measure.units.volume.VolumeUnits;

/**
 * The volume dimension. Base unit is liters.
 */
public enum Volume implements Dimension {

    INSTANCE;

    /** {@inheritDoc} Returns {@link VolumeUnits#LITER}. */
    @Override
    public Unit getBaseUnit() {
        return VolumeUnits.LITER;
    }
}

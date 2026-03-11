package systems.courant.shrewd.measure.dimension;

import systems.courant.shrewd.measure.Dimension;
import systems.courant.shrewd.measure.Unit;
import systems.courant.shrewd.measure.units.volume.VolumeUnits;

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

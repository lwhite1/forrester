package systems.courant.sd.measure.dimension;

import systems.courant.sd.measure.Dimension;
import systems.courant.sd.measure.Unit;
import systems.courant.sd.measure.units.volume.VolumeUnits;

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

package systems.courant.sd.measure.dimension;

import systems.courant.sd.measure.Dimension;
import systems.courant.sd.measure.Unit;
import systems.courant.sd.measure.units.mass.MassUnits;

/**
 * The mass dimension. Base unit is kilograms.
 */
public enum Mass implements Dimension {

    INSTANCE;

    /** {@inheritDoc} Returns {@link MassUnits#KILOGRAM}. */
    @Override
    public Unit getBaseUnit() {
        return MassUnits.KILOGRAM;
    }
}

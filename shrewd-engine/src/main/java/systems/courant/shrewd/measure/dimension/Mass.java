package systems.courant.shrewd.measure.dimension;

import systems.courant.shrewd.measure.Dimension;
import systems.courant.shrewd.measure.Unit;
import systems.courant.shrewd.measure.units.mass.MassUnits;

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

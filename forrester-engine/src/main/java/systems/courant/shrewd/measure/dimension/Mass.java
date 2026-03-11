package systems.courant.forrester.measure.dimension;

import systems.courant.forrester.measure.Dimension;
import systems.courant.forrester.measure.Unit;
import systems.courant.forrester.measure.units.mass.MassUnits;

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

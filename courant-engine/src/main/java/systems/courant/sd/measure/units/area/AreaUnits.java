package systems.courant.sd.measure.units.area;

import systems.courant.sd.measure.Dimension;
import systems.courant.sd.measure.Unit;

/**
 * Standard units of area. Each constant stores its ratio to the base unit (square meters).
 */
public enum AreaUnits implements Unit {

    SQUARE_METER("m²", 1.0),
    HECTARE("hectare", 10_000.0),
    SQUARE_KILOMETER("km²", 1_000_000.0),
    ACRE("acre", 4_046.8564224);

    private final String name;
    private final double ratioToBaseUnit;

    AreaUnits(String name, double ratioToBaseUnit) {
        this.name = name;
        this.ratioToBaseUnit = ratioToBaseUnit;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return name;
    }

    /** {@inheritDoc} Returns {@link Dimension#AREA}. */
    @Override
    public Dimension getDimension() {
        return Dimension.AREA;
    }

    /** {@inheritDoc} */
    @Override
    public double ratioToBaseUnit() {
        return ratioToBaseUnit;
    }
}

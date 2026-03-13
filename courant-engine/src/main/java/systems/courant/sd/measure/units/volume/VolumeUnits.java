package systems.courant.sd.measure.units.volume;

import systems.courant.sd.measure.Dimension;
import systems.courant.sd.measure.Unit;

/**
 * Standard units of volume. Each constant stores its ratio to the base unit (liters).
 * Conversion factors use the exact definitions where applicable.
 */
public enum VolumeUnits implements Unit {

    MILLILITER("Milliliter", 0.001),
    LITER("Liter", 1.0),
    CUBIC_METER("Cubic Meter", 1000.0),
    FLUID_OUNCE_US("Fluid Ounce (US)", 0.0295735295625),
    CUP_US("Cup (US)", 0.2365882365),
    PINT_US("Pint (US)", 0.473176473),
    QUART_US("Quart (US)", 0.946352946),
    GALLON_US("Gallon (US)", 3.785411784),
    IMPERIAL_GALLON("Imperial Gallon", 4.54609),
    BARREL("Barrel", 158.987294928);

    private final String name;
    private final double ratioToBaseUnit;

    VolumeUnits(String name, double ratioToBaseUnit) {
        this.name = name;
        this.ratioToBaseUnit = ratioToBaseUnit;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return name;
    }

    /** {@inheritDoc} Returns {@link Dimension#VOLUME}. */
    @Override
    public Dimension getDimension() {
        return Dimension.VOLUME;
    }

    /** {@inheritDoc} */
    @Override
    public double ratioToBaseUnit() {
        return ratioToBaseUnit;
    }
}

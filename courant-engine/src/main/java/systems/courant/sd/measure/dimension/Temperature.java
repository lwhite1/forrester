package systems.courant.sd.measure.dimension;

import systems.courant.sd.measure.Converter;
import systems.courant.sd.measure.Dimension;
import systems.courant.sd.measure.Unit;
import systems.courant.sd.measure.units.temperature.TemperatureUnits;

/**
 * The temperature dimension. Base unit is Celsius.
 *
 * <p>Unit conversion between Celsius and Fahrenheit is not supported because it requires
 * an affine transformation (offset + scale) that the ratio-based converter cannot express.
 */
public enum Temperature implements Dimension {

    INSTANCE;

    /** {@inheritDoc} Returns {@link TemperatureUnits#CELSIUS}. */
    @Override
    public Unit getBaseUnit() {
        return TemperatureUnits.CELSIUS;
    }

    /**
     * Always throws {@link UnsupportedOperationException} because temperature conversion
     * requires an affine transformation that the ratio-based converter cannot express.
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    public Converter getConverter() {
        throw new UnsupportedOperationException("There is no converter (yet) for temperatures");
    }
}

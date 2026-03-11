package systems.courant.forrester.measure;

import systems.courant.forrester.measure.dimension.Item;
import systems.courant.forrester.measure.dimension.Length;
import systems.courant.forrester.measure.dimension.Mass;
import systems.courant.forrester.measure.dimension.Money;
import systems.courant.forrester.measure.dimension.Temperature;
import systems.courant.forrester.measure.dimension.Time;
import systems.courant.forrester.measure.dimension.Volume;

/**
 * Represents a physical or conceptual dimension such as time, length, mass, or money.
 * Each dimension has a base unit and provides a converter for translating quantities
 * between compatible units within the same dimension.
 */
public interface Dimension {

    Dimension TIME = Time.INSTANCE;
    Dimension MONEY = Money.INSTANCE;
    Dimension MASS = Mass.INSTANCE;
    Dimension LENGTH = Length.INSTANCE;
    Dimension VOLUME = Volume.INSTANCE;
    Dimension ITEM = Item.INSTANCE;
    Dimension TEMPERATURE = Temperature.INSTANCE;

    /**
     * Returns a converter that can translate quantities between units in this dimension.
     * Throws {@link IllegalArgumentException} if the target unit belongs to a different dimension.
     */
    default Converter getConverter() {
        return (originalQuantity, newUnit) -> {
            if (!newUnit.getDimension().equals(originalQuantity.getUnit().getDimension())) {
                throw new IllegalArgumentException("Cannot convert between incompatible dimensions");
            }
            return newUnit.fromBaseUnits(originalQuantity.inBaseUnits());
        };
    }

    /**
     * Returns the base unit for this dimension. All conversions pass through the base unit.
     */
    Unit getBaseUnit();
}

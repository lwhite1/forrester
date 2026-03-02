package com.deathrayresearch.forrester.measure;

import com.deathrayresearch.forrester.measure.dimension.Item;
import com.deathrayresearch.forrester.measure.dimension.Length;
import com.deathrayresearch.forrester.measure.dimension.Mass;
import com.deathrayresearch.forrester.measure.dimension.Money;
import com.deathrayresearch.forrester.measure.dimension.Temperature;
import com.deathrayresearch.forrester.measure.dimension.Time;
import com.deathrayresearch.forrester.measure.dimension.Volume;

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

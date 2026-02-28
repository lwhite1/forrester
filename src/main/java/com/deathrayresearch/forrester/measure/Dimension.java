package com.deathrayresearch.forrester.measure;

import com.deathrayresearch.forrester.measure.dimension.Item;
import com.deathrayresearch.forrester.measure.dimension.Length;
import com.deathrayresearch.forrester.measure.dimension.Mass;
import com.deathrayresearch.forrester.measure.dimension.Money;
import com.deathrayresearch.forrester.measure.dimension.Temperature;
import com.deathrayresearch.forrester.measure.dimension.Time;
import com.deathrayresearch.forrester.measure.dimension.Volume;

/**
 *
 */
public interface Dimension {

    Dimension TIME = Time.getInstance();
    Dimension MONEY = Money.getInstance();
    Dimension MASS = Mass.getInstance();
    Dimension LENGTH = Length.getInstance();
    Dimension VOLUME = Volume.getInstance();
    Dimension ITEM = Item.getInstance();
    Dimension TEMPERATURE = Temperature.getInstance();

    default Converter getConverter() {
        return (originalQuantity, newUnit) -> {
            if (!newUnit.getDimension().equals(originalQuantity.getUnit().getDimension())) {
                throw new IllegalArgumentException("Cannot convert between incompatible dimensions");
            }
            return newUnit.fromBaseUnits(originalQuantity.inBaseUnits());
        };
    }

    Unit getBaseUnit();
}

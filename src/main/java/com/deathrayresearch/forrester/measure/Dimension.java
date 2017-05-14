package com.deathrayresearch.forrester.measure;

import com.deathrayresearch.forrester.measure.dimension.Item;
import com.deathrayresearch.forrester.measure.dimension.Length;
import com.deathrayresearch.forrester.measure.dimension.Mass;
import com.deathrayresearch.forrester.measure.dimension.Money;
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

    Converter getConverter();

    Unit getBaseUnit();
}

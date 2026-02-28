package com.deathrayresearch.forrester.measure.dimension;


import com.deathrayresearch.forrester.measure.Dimension;
import com.deathrayresearch.forrester.measure.Unit;
import com.deathrayresearch.forrester.measure.units.item.Thing;

/**
 *
 */
public class Item implements Dimension {

    private static final Item ourInstance = new Item();

    public static Item getInstance() {
        return ourInstance;
    }

    private Item() {}

    @Override
    public Unit getBaseUnit() {
        return Thing.getInstance();
    }

}

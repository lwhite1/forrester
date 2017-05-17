package com.deathrayresearch.forrester.largemodels.waterfall.units;


import com.deathrayresearch.forrester.measure.Dimension;
import com.deathrayresearch.forrester.measure.Unit;
import com.deathrayresearch.forrester.measure.dimension.Item;

/**
 *
 */

public class TasksPerPersonDay implements Unit {

    private static final TasksPerPersonDay instance = new TasksPerPersonDay();

    @Override
    public String getName() {
        return "Tasks per person day";
    }

    @Override
    public Dimension getDimension() {
        return Item.getInstance();
    }

    @Override
    public double ratioToBaseUnit() {
        return 1.0;
    }

    public static TasksPerPersonDay getInstance() {
        return instance;
    }
}

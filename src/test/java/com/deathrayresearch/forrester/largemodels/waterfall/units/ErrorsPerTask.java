package com.deathrayresearch.forrester.largemodels.waterfall.units;


import com.deathrayresearch.forrester.measure.Dimension;
import com.deathrayresearch.forrester.measure.Unit;
import com.deathrayresearch.forrester.measure.dimension.Item;

/**
 *
 */
public class ErrorsPerTask implements Unit {

    private static final ErrorsPerTask instance = new ErrorsPerTask();

    @Override
    public String getName() {
        return "Errors per task";
    }

    @Override
    public Dimension getDimension() {
        return Item.getInstance();
    }

    @Override
    public double ratioToBaseUnit() {
        return 1.0;
    }

    public static ErrorsPerTask getInstance() {
        return instance;
    }
}

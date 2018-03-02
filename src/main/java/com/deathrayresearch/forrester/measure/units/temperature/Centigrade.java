package com.deathrayresearch.forrester.measure.units.temperature;

import com.deathrayresearch.forrester.measure.Dimension;
import com.deathrayresearch.forrester.measure.Unit;
import com.deathrayresearch.forrester.measure.units.mass.Kilogram;

public class Centigrade implements Unit {

    public static final String NAME = "Centigrade";
    public static final Dimension DIMENSION = Dimension.TEMPERATURE;
    private static final Centigrade instance = new Centigrade();

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Dimension getDimension() {
        return DIMENSION;
    }

    @Override
    public double ratioToBaseUnit() {
        return 1.0;
    }

    public static Centigrade getInstance() {
        return instance;
    }

}

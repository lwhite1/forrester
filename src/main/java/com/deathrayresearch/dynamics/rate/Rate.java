package com.deathrayresearch.dynamics.rate;

import com.deathrayresearch.dynamics.measure.Dimension;
import com.deathrayresearch.dynamics.measure.Quantity;
import com.deathrayresearch.dynamics.measure.Unit;
import com.deathrayresearch.dynamics.measure.dimension.Time;

/**
 * Rate is a function that returns the quantity with unit Q that "flows" into a stock in a give timeUnit
 */
public interface Rate<Q extends Dimension> {

    Quantity<Q> flowPerTimeUnit(Unit<Time> timeUnit);

}

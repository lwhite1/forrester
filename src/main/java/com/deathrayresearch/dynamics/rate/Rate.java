package com.deathrayresearch.dynamics.rate;

import com.deathrayresearch.dynamics.measure.Dimension;
import com.deathrayresearch.dynamics.measure.Quantity;
import com.deathrayresearch.dynamics.measure.Unit;
import com.deathrayresearch.dynamics.measure.dimension.Time;

/**
 *
 */
public interface Rate<Q extends Dimension> {

    Quantity<Q> flowPerTimeUnit(Unit<Time> timeUnit);
}

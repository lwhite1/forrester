package com.deathrayresearch.forrester.rate;


import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.TimeUnit;

/**
 * Rate is a function that returns the quantity with unit Q that "flows" into a stock in a give timeUnit
 */
public interface Rate {

    Quantity flowPerTimeUnit(TimeUnit timeUnit);

}

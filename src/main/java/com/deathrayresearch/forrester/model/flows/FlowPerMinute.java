package com.deathrayresearch.forrester.model.flows;

import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.TimeUnit;
import com.deathrayresearch.forrester.model.Flow;

import static com.deathrayresearch.forrester.measure.Units.MINUTE;

/**
 *
 */
public abstract class FlowPerMinute extends Flow {

    public FlowPerMinute(String name) {
        super(name, MINUTE);
    }

    private Quantity convert(Quantity quantity, TimeUnit newTimeUnit) {
        return RateConverter.convert(quantity, getTimeUnit(), newTimeUnit);
    }

    @Override
    public Quantity flowPerTimeUnit(TimeUnit timeUnit) {
        return convert(quantityPerMinute(), timeUnit);
    }

    protected abstract Quantity quantityPerMinute();

}

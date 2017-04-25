package com.deathrayresearch.dynamics.model;

import com.deathrayresearch.dynamics.measure.Dimension;
import com.deathrayresearch.dynamics.rate.Rate;

/**
 *
 */
public class Flow<E extends Dimension> extends Element {

    private Rate rate;

    public Rate getRate() {
        return rate;
    }

    public void setRate(Rate<E> rate) {
        this.rate = rate;
    }

    public Flow(String name, Rate<E> rate) {
        super(name);
        this.rate = rate;
    }


}

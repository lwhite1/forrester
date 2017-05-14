package com.deathrayresearch.forrester.model;


import com.deathrayresearch.forrester.rate.Rate;

/**
 * A flow of materials, money, etc., out of one stock and/or into another
 */
public class Flow extends Element {

    private Rate rate;

    public Rate getRate() {
        return rate;
    }

    public void setRate(Rate rate) {
        this.rate = rate;
    }

    public String getName() {
        return super.getName();
    }

    public Flow(String name, Rate rate) {
        super(name);
        this.rate = rate;
    }
}
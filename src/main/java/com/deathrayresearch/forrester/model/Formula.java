package com.deathrayresearch.forrester.model;

/**
 * A formula is a calculation that returns the current value of its variable
 */
public interface Formula {

    /**
     * Computes and returns the current value of the variable that uses this formula.
     */
    double getCurrentValue();

}

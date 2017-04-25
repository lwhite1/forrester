package com.deathrayresearch.dynamics.model;

import com.deathrayresearch.dynamics.measure.Dimension;

/**
 *
 */
interface Sink<E extends Dimension> {

    void addInflow(Flow<E> inflow);
}

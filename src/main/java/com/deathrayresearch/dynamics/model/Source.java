package com.deathrayresearch.dynamics.model;

import com.deathrayresearch.dynamics.measure.Dimension;

/**
 *
 */
interface Source<E extends Dimension> {

    void addOutflow(Flow<E> outflow);
}

package com.deathrayresearch.dynamics.model;

import java.util.HashSet;
import java.util.Set;

/**
 *
 */
public class ExternalSink extends Element implements Sink {

    private Set<Flow> inflows = new HashSet<>();

    public ExternalSink(String name) {
        super(name);
    }

    @Override
    public void addInflow(Flow inflow) {

    }
}

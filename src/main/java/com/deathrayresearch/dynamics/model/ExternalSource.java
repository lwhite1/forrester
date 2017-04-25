package com.deathrayresearch.dynamics.model;

import java.util.HashSet;
import java.util.Set;

/**
 *
 */
public class ExternalSource extends Element implements Source {

    private Set<Flow> outflows = new HashSet<>();

    public ExternalSource(String name) {
        super(name);
    }

    @Override
    public void addOutflow(Flow outflow) {
        outflows.add(outflow);
    }
}

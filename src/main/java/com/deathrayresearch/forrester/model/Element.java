package com.deathrayresearch.forrester.model;

import com.google.common.base.Preconditions;

/**
 *  Parent of all model elements: Stock, Flow, etc.
 */
public abstract class Element {

    private final String name;
    private String comment;

    protected Element(String name) {
        Preconditions.checkArgument(name != null, "name cannot be null");
        Preconditions.checkArgument(!name.isEmpty(), "name cannot be empty");
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return getName();
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}

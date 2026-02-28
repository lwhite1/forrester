package com.deathrayresearch.forrester.model;

/**
 *  Parent of all model elements: Stock, Flow, etc.
 */
abstract class Element {

    private final String name;
    private String comment;

    protected Element(String name) {
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

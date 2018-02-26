package com.deathrayresearch.forrester.model;

/**
 *  Parent of all model elements: Stock, Flow, etc.
 */
abstract class Element {

    private String comment;

    public abstract String getName();

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

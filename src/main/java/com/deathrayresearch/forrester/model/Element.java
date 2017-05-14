package com.deathrayresearch.forrester.model;

/**
 *  Parent of all model elements: Stock, Flow, etc.
 */
abstract class Element {

    private String name;
    private String comment;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Element(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}

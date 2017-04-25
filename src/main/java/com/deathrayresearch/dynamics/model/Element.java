package com.deathrayresearch.dynamics.model;

/**
 *  Parent of all model elements: Stock, Flow, etc.
 */
abstract class Element {

    private String name;

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
}

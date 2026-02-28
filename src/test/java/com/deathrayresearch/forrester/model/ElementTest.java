package com.deathrayresearch.forrester.model;

import org.junit.Test;

import static com.deathrayresearch.forrester.measure.Units.THING;

public class ElementTest {

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectNullName() {
        new Stock(null, 0, THING);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectEmptyName() {
        new Stock("", 0, THING);
    }

    @Test
    public void shouldAcceptValidName() {
        Stock stock = new Stock("valid", 0, THING);
        org.junit.Assert.assertEquals("valid", stock.getName());
    }
}

package com.deathrayresearch.forrester.model;

import org.junit.jupiter.api.Test;

import static com.deathrayresearch.forrester.measure.Units.THING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ElementTest {

    @Test
    public void shouldRejectNullName() {
        assertThrows(IllegalArgumentException.class, () -> new Stock(null, 0, THING));
    }

    @Test
    public void shouldRejectEmptyName() {
        assertThrows(IllegalArgumentException.class, () -> new Stock("", 0, THING));
    }

    @Test
    public void shouldAcceptValidName() {
        Stock stock = new Stock("valid", 0, THING);
        assertEquals("valid", stock.getName());
    }
}

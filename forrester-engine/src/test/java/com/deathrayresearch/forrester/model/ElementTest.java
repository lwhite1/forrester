package com.deathrayresearch.forrester.model;

import org.junit.jupiter.api.Test;

import static com.deathrayresearch.forrester.measure.Units.THING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    public void shouldReturnNullCommentByDefault() {
        Stock stock = new Stock("test", 0, THING);
        assertNull(stock.getComment());
    }

    @Test
    public void shouldStoreAndReturnComment() {
        Stock stock = new Stock("test", 0, THING);
        stock.setComment("A test comment");
        assertEquals("A test comment", stock.getComment());
    }

    @Test
    public void shouldReturnNameFromToString() {
        Stock stock = new Stock("myStock", 0, THING);
        String result = stock.toString();
        assertTrue(result.contains("myStock"));
    }
}

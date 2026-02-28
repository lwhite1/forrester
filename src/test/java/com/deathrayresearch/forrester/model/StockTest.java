package com.deathrayresearch.forrester.model;

import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.Unit;
import org.junit.jupiter.api.Test;

import static com.deathrayresearch.forrester.measure.Units.GALLON_US;
import static com.deathrayresearch.forrester.measure.Units.MINUTE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StockTest {

    @Test
    public void shouldStoreInitialValue() {
        Stock stock = new Stock("Water", 100, GALLON_US);
        assertEquals(100, stock.getValue(), 0.0);
        assertEquals(GALLON_US, stock.getUnit());
    }

    @Test
    public void shouldUpdateValue() {
        Stock stock = new Stock("Water", 100, GALLON_US);
        stock.setValue(50);
        assertEquals(50, stock.getValue(), 0.0);
    }

    @Test
    public void shouldTrackInflows() {
        Stock stock = new Stock("Water", 0, GALLON_US);
        Flow inflow = createConstantFlow("Fill", 5, GALLON_US);
        stock.addInflow(inflow);

        assertEquals(1, stock.getInflows().size());
        assertTrue(stock.getInflows().contains(inflow));
        assertEquals(stock, inflow.getSink());
    }

    @Test
    public void shouldTrackOutflows() {
        Stock stock = new Stock("Water", 100, GALLON_US);
        Flow outflow = createConstantFlow("Drain", 5, GALLON_US);
        stock.addOutflow(outflow);

        assertEquals(1, stock.getOutflows().size());
        assertTrue(stock.getOutflows().contains(outflow));
        assertEquals(stock, outflow.getSource());
    }

    @Test
    public void shouldReturnUnmodifiableInflows() {
        Stock stock = new Stock("Water", 0, GALLON_US);
        assertThrows(UnsupportedOperationException.class, () -> {
            stock.getInflows().add(createConstantFlow("X", 1, GALLON_US));
        });
    }

    @Test
    public void shouldReturnUnmodifiableOutflows() {
        Stock stock = new Stock("Water", 0, GALLON_US);
        assertThrows(UnsupportedOperationException.class, () -> {
            stock.getOutflows().add(createConstantFlow("X", 1, GALLON_US));
        });
    }

    @Test
    public void shouldReturnNameFromToString() {
        Stock stock = new Stock("Tank", 10, GALLON_US);
        assertTrue(stock.toString().contains("Tank"));
    }

    private static Flow createConstantFlow(String name, double value, Unit unit) {
        return Flow.create(name, MINUTE, () -> new Quantity(value, unit));
    }
}

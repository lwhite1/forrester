package com.deathrayresearch.forrester.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.deathrayresearch.forrester.measure.Units.MINUTE;
import static com.deathrayresearch.forrester.measure.Units.THING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ModuleTest {

    private Module module;

    @BeforeEach
    public void setUp() {
        module = new Module("Test Module");
    }

    @Test
    public void shouldStoreName() {
        assertEquals("Test Module", module.getName());
    }

    @Test
    public void shouldAddAndRetrieveStock() {
        Stock stock = new Stock("S1", 25, THING);
        module.addStock(stock);
        assertEquals(stock, module.getStock("S1"));
        assertEquals(25.0, module.valueOfStock("S1").getValue(), 0.0);
    }

    @Test
    public void shouldFailOnMissingStockName() {
        assertThrows(IllegalArgumentException.class, () -> module.valueOfStock("nonexistent"));
    }

    @Test
    public void shouldFailOnMissingFlowName() {
        assertThrows(IllegalArgumentException.class, () -> module.valueOfFlow("nonexistent", MINUTE));
    }

    @Test
    public void shouldReturnNullForMissingStockViaGet() {
        assertNull(module.getStock("nonexistent"));
    }

    @Test
    public void shouldAddAndRetrieveVariable() {
        Variable var = new Variable("V1", THING, () -> 99.0);
        module.addVariable(var);
        assertEquals(var, module.getVariable("V1"));
        assertEquals(1, module.getVariables().size());
    }

    @Test
    public void shouldReturnNullForMissingVariableViaGet() {
        assertNull(module.getVariable("nonexistent"));
    }
}

package com.deathrayresearch.forrester.model;

import org.junit.Before;
import org.junit.Test;

import static com.deathrayresearch.forrester.measure.Units.MINUTE;
import static com.deathrayresearch.forrester.measure.Units.THING;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ModuleTest {

    private Module module;

    @Before
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

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailOnMissingStockName() {
        module.valueOfStock("nonexistent");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailOnMissingFlowName() {
        module.valueOfFlow("nonexistent", MINUTE);
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

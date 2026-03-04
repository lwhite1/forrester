package com.deathrayresearch.forrester.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.deathrayresearch.forrester.measure.Units.MINUTE;
import static com.deathrayresearch.forrester.measure.Units.THING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
        assertThat(module.getStock("S1")).isPresent().contains(stock);
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
        assertThat(module.getStock("nonexistent")).isEmpty();
    }

    @Test
    public void shouldAddAndRetrieveVariable() {
        Variable var = new Variable("V1", THING, () -> 99.0);
        module.addVariable(var);
        assertThat(module.getVariable("V1")).isPresent().contains(var);
        assertEquals(1, module.getVariables().size());
    }

    @Test
    public void shouldReturnNullForMissingVariableViaGet() {
        assertThat(module.getVariable("nonexistent")).isEmpty();
    }
}

package com.deathrayresearch.forrester.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.deathrayresearch.forrester.measure.Units.THING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ModelTest {

    private Model model;

    @BeforeEach
    public void setUp() {
        model = new Model("Test Model");
    }

    @Test
    public void shouldStoreName() {
        assertEquals("Test Model", model.getName());
    }

    @Test
    public void shouldAddAndRemoveStocks() {
        Stock stock = new Stock("S1", 10, THING);
        model.addStock(stock);
        assertEquals(1, model.getStocks().size());
        assertEquals("S1", model.getStockNames().get(0));
        assertEquals(10.0, model.getStockValues().get(0), 0.0);

        model.removeStock(stock);
        assertTrue(model.getStocks().isEmpty());
    }

    @Test
    public void shouldAddAndRemoveVariables() {
        Variable var = new Variable("V1", THING, () -> 42.0);
        model.addVariable(var);
        assertEquals(1, model.getVariables().size());
        assertEquals("V1", model.getVariableNames().get(0));
        assertEquals(42.0, model.getVariableValues().get(0), 0.0);
        assertThat(model.getVariable("V1")).isPresent().contains(var);

        model.removeVariable(var);
        assertTrue(model.getVariables().isEmpty());
    }

    @Test
    public void shouldReturnNullForMissingVariable() {
        assertThat(model.getVariable("nonexistent")).isEmpty();
    }

    @Test
    public void shouldAddModules() {
        Module module = new Module("M1");
        model.addModule(module);
        assertEquals(1, model.getModules().size());
        assertEquals("M1", model.getModuleNames().get(0));
    }

    @Test
    public void shouldAddConstants() {
        Constant c = new Constant("C1", THING, 3.14);
        model.addConstant(c);
        assertEquals(1, model.getConstants().size());
    }

    @Test
    public void shouldReturnUnmodifiableStocks() {
        assertThrows(UnsupportedOperationException.class, () -> {
            model.getStocks().add(new Stock("X", 0, THING));
        });
    }

    @Test
    public void shouldReturnUnmodifiableModules() {
        assertThrows(UnsupportedOperationException.class, () -> {
            model.getModules().add(new Module("X"));
        });
    }

    @Test
    public void shouldReturnUnmodifiableVariables() {
        assertThrows(UnsupportedOperationException.class, () -> {
            model.getVariables().add(new Variable("X", THING, () -> 0));
        });
    }

    @Test
    public void shouldReturnUnmodifiableConstants() {
        assertThrows(UnsupportedOperationException.class, () -> {
            model.getConstants().add(new Constant("X", THING, 0));
        });
    }
}

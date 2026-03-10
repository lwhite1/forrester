package systems.courant.forrester.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static systems.courant.forrester.measure.Units.THING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    public void shouldAddVariablesAsConstants() {
        Variable v = new Variable("C1", THING, () -> 3.14);
        model.addVariable(v);
        assertEquals(1, model.getVariables().size());
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
    public void shouldWarnOnModuleWithCollidingStockName() {
        Stock stock = new Stock("Population", 100, THING);
        model.addStock(stock);

        Module module = new Module("SubModel");
        module.addStock(new Stock("Population", 200, THING));

        // Stock collisions log a warning but do not throw
        model.addModule(module);
        assertThat(model.getStocks()).hasSize(2);
    }

    @Test
    public void shouldRejectModuleWithCollidingVariableName() {
        model.addVariable(new Variable("Rate", THING, () -> 0.5));

        Module module = new Module("SubModel");
        module.addVariable(new Variable("Rate", THING, () -> 0.3));

        assertThatThrownBy(() -> model.addModule(module))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Rate")
                .hasMessageContaining("collides");
    }

    @Test
    public void shouldAllowSameObjectInModuleWithoutCollision() {
        Stock stock = new Stock("Shared", 100, THING);
        model.addStock(stock);

        Module module = new Module("SubModel");
        module.addStock(stock); // same object, not a collision

        model.addModule(module); // should not throw
        assertThat(model.getStocks()).hasSize(1);
    }
}

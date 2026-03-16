package systems.courant.sd.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import systems.courant.sd.measure.Quantity;

import static systems.courant.sd.measure.Units.MINUTE;
import static systems.courant.sd.measure.Units.THING;
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
    public void shouldWarnButAddModuleStockWithCollidingName() {
        Stock stock = new Stock("Population", 100, THING);
        model.addStock(stock);

        Module module = new Module("SubModel");
        module.addStock(new Stock("Population", 200, THING));

        // Different object with same name: added with a warning (supports multi-instance modules)
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

    @Test
    public void shouldRejectDuplicateStockName() {
        model.addStock(new Stock("Population", 100, THING));

        assertThatThrownBy(() -> model.addStock(new Stock("Population", 200, THING)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Population")
                .hasMessageContaining("Duplicate stock name");
    }

    @Test
    public void shouldRejectDuplicateFlowName() {
        model.addFlow(Flow.create("Birth Rate", MINUTE, () -> new Quantity(10, THING)));

        assertThatThrownBy(() -> model.addFlow(Flow.create("Birth Rate", MINUTE, () -> new Quantity(5, THING))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Birth Rate")
                .hasMessageContaining("Duplicate flow name");
    }

    @Test
    public void shouldRejectDuplicateVariableName() {
        model.addVariable(new Variable("Rate", THING, () -> 0.5));

        assertThatThrownBy(() -> model.addVariable(new Variable("Rate", THING, () -> 0.3)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Rate")
                .hasMessageContaining("Duplicate variable name");
    }

    @Test
    public void shouldAllowReAddingSameVariableObject() {
        Variable v = new Variable("Rate", THING, () -> 0.5);
        model.addVariable(v);
        model.addVariable(v); // same object — should not throw
        assertThat(model.getVariables()).hasSize(1);
    }

    @Test
    public void shouldAllowReAddingStockAfterRemoval() {
        Stock stock = new Stock("Population", 100, THING);
        model.addStock(stock);
        model.removeStock(stock);
        // After removal, re-adding a stock with the same name should succeed
        model.addStock(new Stock("Population", 200, THING));
        assertThat(model.getStocks()).hasSize(1);
    }

    @Test
    public void shouldHandleManyStocksEfficiently() {
        for (int i = 0; i < 1000; i++) {
            model.addStock(new Stock("Stock" + i, i, THING));
        }
        assertThat(model.getStocks()).hasSize(1000);
        assertThatThrownBy(() -> model.addStock(new Stock("Stock500", 0, THING)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void shouldHandleManyFlowsEfficiently() {
        for (int i = 0; i < 1000; i++) {
            model.addFlow(Flow.create("Flow" + i, MINUTE, () -> new Quantity(0, THING)));
        }
        assertThat(model.getFlows()).hasSize(1000);
        assertThatThrownBy(() -> model.addFlow(Flow.create("Flow500", MINUTE, () -> new Quantity(0, THING))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void shouldWarnButAddModuleFlowWithCollidingName() {
        model.addFlow(Flow.create("Birth Rate", MINUTE, () -> new Quantity(10, THING)));

        Module module = new Module("SubModel");
        module.addFlow(Flow.create("Birth Rate", MINUTE, () -> new Quantity(5, THING)));

        // Different object with same name: added with a warning (supports multi-instance modules)
        model.addModule(module);
        assertThat(model.getFlows()).hasSize(2);
    }
}

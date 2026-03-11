package systems.courant.shrewd.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static systems.courant.shrewd.measure.Units.MINUTE;
import static systems.courant.shrewd.measure.Units.THING;
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

    @Test
    public void shouldRejectDirectSelfReference() {
        assertThrows(IllegalArgumentException.class, () -> module.addSubModule(module));
    }

    @Test
    public void shouldRejectIndirectCycle() {
        Module a = new Module("A");
        Module b = new Module("B");
        Module c = new Module("C");
        a.addSubModule(b);
        b.addSubModule(c);

        // c → a would create A→B→C→A cycle
        assertThrows(IllegalArgumentException.class, () -> c.addSubModule(a));
    }

    @Test
    public void shouldAllowDiamondWithoutCycle() {
        Module a = new Module("A");
        Module b = new Module("B");
        Module c = new Module("C");
        Module d = new Module("D");
        a.addSubModule(b);
        a.addSubModule(c);
        b.addSubModule(d);
        // c → d is fine (diamond shape, not a cycle)
        c.addSubModule(d);

        assertThat(c.getSubModule("D")).isPresent();
    }
}

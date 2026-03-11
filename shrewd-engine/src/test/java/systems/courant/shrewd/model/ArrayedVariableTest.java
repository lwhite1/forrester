package systems.courant.shrewd.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static systems.courant.shrewd.measure.Units.PEOPLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ArrayedVariableTest {

    private final Subscript region = new Subscript("Region", "North", "South", "East");

    @Test
    public void shouldExpandToNVariables() {
        ArrayedVariable av = ArrayedVariable.create("Pop", PEOPLE, region, i -> (i + 1) * 100.0);
        assertEquals(3, av.size());
    }

    @Test
    public void shouldNameVariablesWithBracketConvention() {
        ArrayedVariable av = ArrayedVariable.create("Pop", PEOPLE, region, i -> 0);
        List<Variable> vars = av.getVariables();
        assertEquals("Pop[North]", vars.get(0).getName());
        assertEquals("Pop[South]", vars.get(1).getName());
        assertEquals("Pop[East]", vars.get(2).getName());
    }

    @Test
    public void shouldComputeFormulaPerIndex() {
        ArrayedVariable av = ArrayedVariable.create("Pop", PEOPLE, region, i -> (i + 1) * 100.0);
        assertEquals(100, av.getValue(0), 0.0);
        assertEquals(200, av.getValue(1), 0.0);
        assertEquals(300, av.getValue(2), 0.0);
    }

    @Test
    public void shouldAccessByLabel() {
        ArrayedVariable av = ArrayedVariable.create("Pop", PEOPLE, region, i -> (i + 1) * 100.0);
        assertEquals(100, av.getValue("North"), 0.0);
        assertEquals(200, av.getValue("South"), 0.0);
        assertEquals(300, av.getValue("East"), 0.0);
    }

    @Test
    public void shouldAccessVariableByLabel() {
        ArrayedVariable av = ArrayedVariable.create("Pop", PEOPLE, region, i -> 42);
        Variable v = av.getVariable("South");
        assertEquals("Pop[South]", v.getName());
        assertEquals(42, v.getValue(), 0.0);
    }

    @Test
    public void shouldComputeSum() {
        ArrayedVariable av = ArrayedVariable.create("Pop", PEOPLE, region, i -> (i + 1) * 100.0);
        assertEquals(600, av.sum(), 0.0);
    }

    @Test
    public void shouldReturnUnmodifiableList() {
        ArrayedVariable av = ArrayedVariable.create("Pop", PEOPLE, region, i -> 0);
        assertThrows(UnsupportedOperationException.class,
                () -> av.getVariables().add(new Variable("X", PEOPLE, () -> 0)));
    }

    @Test
    public void shouldReturnMetadata() {
        ArrayedVariable av = ArrayedVariable.create("Pop", PEOPLE, region, i -> 0);
        assertEquals("Pop", av.getBaseName());
        assertEquals(region, av.getSubscript());
    }

    @Test
    public void shouldReflectDynamicFormulaValues() {
        double[] values = {10, 20, 30};
        ArrayedVariable av = ArrayedVariable.create("V", PEOPLE, region, i -> values[i]);
        assertEquals(10, av.getValue(0), 0.0);

        // Modify underlying data
        values[0] = 99;
        assertEquals(99, av.getValue(0), 0.0);
    }

    @Test
    public void toStringShouldContainBaseName() {
        ArrayedVariable av = ArrayedVariable.create("Pop", PEOPLE, region, i -> 0);
        assertTrue(av.toString().contains("Pop"));
    }

    private void assertTrue(boolean condition) {
        org.junit.jupiter.api.Assertions.assertTrue(condition);
    }
}

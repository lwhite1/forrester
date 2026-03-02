package com.deathrayresearch.forrester.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static com.deathrayresearch.forrester.measure.Units.PEOPLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class MultiArrayedVariableTest {

    private final Subscript region = new Subscript("Region", "North", "South", "East");
    private final Subscript ageGroup = new Subscript("AgeGroup", "Young", "Adult", "Elder");
    private final SubscriptRange range = new SubscriptRange(List.of(region, ageGroup));

    @Test
    public void shouldExpandToCorrectNumberOfVariables() {
        MultiArrayedVariable var = MultiArrayedVariable.create("Density", PEOPLE, range,
                coords -> coords[0] * 10.0 + coords[1]);
        assertEquals(9, var.size());
        assertEquals(9, var.getVariables().size());
    }

    @Test
    public void shouldNameVariablesWithCommaSeparatedLabels() {
        MultiArrayedVariable var = MultiArrayedVariable.create("Density", PEOPLE, range,
                coords -> 0);
        assertEquals("Density[North,Young]", var.getVariable(0).getName());
        assertEquals("Density[South,Elder]", var.getVariable(5).getName());
        assertEquals("Density[East,Elder]", var.getVariable(8).getName());
    }

    @Test
    public void shouldEvaluateCoordinateAwareFormula() {
        MultiArrayedVariable var = MultiArrayedVariable.create("Density", PEOPLE, range,
                coords -> coords[0] * 100.0 + coords[1] * 10.0);

        assertEquals(0, var.getValue(0), 0.001);    // [0,0] → 0
        assertEquals(10, var.getValue(1), 0.001);   // [0,1] → 10
        assertEquals(120, var.getValue(5), 0.001);  // [1,2] → 120
        assertEquals(220, var.getValue(8), 0.001);  // [2,2] → 220
    }

    @Test
    public void shouldEvaluateFlatIndexFormula() {
        MultiArrayedVariable var = MultiArrayedVariable.createByIndex("V", PEOPLE, range,
                i -> i * 5.0);

        assertEquals(0, var.getValue(0), 0.001);
        assertEquals(25, var.getValue(5), 0.001);
        assertEquals(40, var.getValue(8), 0.001);
    }

    @Test
    public void shouldAccessValueByCoordinates() {
        MultiArrayedVariable var = MultiArrayedVariable.create("V", PEOPLE, range,
                coords -> coords[0] * 10.0 + coords[1]);
        assertEquals(0, var.getValueAt(0, 0), 0.001);
        assertEquals(12, var.getValueAt(1, 2), 0.001);
        assertEquals(21, var.getValueAt(2, 1), 0.001);
    }

    @Test
    public void shouldAccessValueByLabels() {
        MultiArrayedVariable var = MultiArrayedVariable.create("V", PEOPLE, range,
                coords -> coords[0] * 10.0 + coords[1]);
        assertEquals(0, var.getValueAt("North", "Young"), 0.001);
        assertEquals(12, var.getValueAt("South", "Elder"), 0.001);
        assertEquals(21, var.getValueAt("East", "Adult"), 0.001);
    }

    @Test
    public void shouldAccessVariableByCoordinates() {
        MultiArrayedVariable var = MultiArrayedVariable.create("V", PEOPLE, range,
                coords -> 0);
        assertEquals("V[South,Adult]", var.getVariableAt(1, 1).getName());
    }

    @Test
    public void shouldAccessVariableByLabels() {
        MultiArrayedVariable var = MultiArrayedVariable.create("V", PEOPLE, range,
                coords -> 0);
        assertEquals("V[East,Young]", var.getVariableAt("East", "Young").getName());
    }

    @Test
    public void shouldComputeSum() {
        // Each element = flat index (0 through 8), sum = 0+1+2+...+8 = 36
        MultiArrayedVariable var = MultiArrayedVariable.createByIndex("V", PEOPLE, range,
                i -> (double) i);
        assertEquals(36, var.sum(), 0.001);
    }

    @Test
    public void shouldSumOverAgeGroupDimension() {
        // coords: [region, age] → value = region*10 + age
        // North: 0+1+2=3, South: 10+11+12=33, East: 20+21+22=63
        MultiArrayedVariable var = MultiArrayedVariable.create("V", PEOPLE, range,
                coords -> coords[0] * 10.0 + coords[1]);
        double[] perRegion = var.sumOver(1);
        assertEquals(3, perRegion.length);
        assertEquals(3, perRegion[0], 0.001);
        assertEquals(33, perRegion[1], 0.001);
        assertEquals(63, perRegion[2], 0.001);
    }

    @Test
    public void shouldSumOverRegionDimension() {
        // coords: [region, age] → value = region*10 + age
        // Young: 0+10+20=30, Adult: 1+11+21=33, Elder: 2+12+22=36
        MultiArrayedVariable var = MultiArrayedVariable.create("V", PEOPLE, range,
                coords -> coords[0] * 10.0 + coords[1]);
        double[] perAge = var.sumOver(0);
        assertEquals(3, perAge.length);
        assertEquals(30, perAge[0], 0.001);
        assertEquals(33, perAge[1], 0.001);
        assertEquals(36, perAge[2], 0.001);
    }

    @Test
    public void shouldReturnUnmodifiableVariableList() {
        MultiArrayedVariable var = MultiArrayedVariable.create("V", PEOPLE, range,
                coords -> 0);
        assertThrows(UnsupportedOperationException.class,
                () -> var.getVariables().add(new Variable("X", PEOPLE, () -> 0)));
    }

    @Test
    public void shouldReturnMetadata() {
        MultiArrayedVariable var = MultiArrayedVariable.create("V", PEOPLE, range,
                coords -> 0);
        assertEquals("V", var.getBaseName());
        assertEquals(PEOPLE, var.getUnit());
        assertEquals(range, var.getRange());
    }
}

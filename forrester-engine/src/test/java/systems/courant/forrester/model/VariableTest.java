package systems.courant.forrester.model;

import org.junit.jupiter.api.Test;

import static systems.courant.forrester.measure.Units.THING;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class VariableTest {

    @Test
    public void shouldReturnFormulaValue() {
        Variable var = new Variable("V1", THING, () -> 42.0);
        assertEquals(42.0, var.getValue(), 0.0);
    }

    @Test
    public void shouldReturnDynamicFormulaValue() {
        double[] counter = {0};
        Variable var = new Variable("Counter", THING, () -> counter[0]++);
        assertEquals(0.0, var.getValue(), 0.0);
        assertEquals(1.0, var.getValue(), 0.0);
        assertEquals(2.0, var.getValue(), 0.0);
    }

    @Test
    public void shouldRecordHistory() {
        Variable var = new Variable("V1", THING, () -> 10.0);
        var.recordValue();
        var.recordValue();
        assertEquals(10.0, var.getHistoryAtTimeStep(0), 0.0);
        assertEquals(10.0, var.getHistoryAtTimeStep(1), 0.0);
    }

    @Test
    public void shouldReturnZeroForOutOfBoundsHistory() {
        Variable var = new Variable("V1", THING, () -> 10.0);
        assertEquals(0.0, var.getHistoryAtTimeStep(-1), 0.0);
        assertEquals(0.0, var.getHistoryAtTimeStep(0), 0.0);
        assertEquals(0.0, var.getHistoryAtTimeStep(100), 0.0);
    }

    @Test
    public void shouldReturnUnit() {
        Variable var = new Variable("V1", THING, () -> 0);
        assertEquals(THING, var.getUnit());
    }
}

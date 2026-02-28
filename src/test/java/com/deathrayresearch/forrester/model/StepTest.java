package com.deathrayresearch.forrester.model;

import org.junit.jupiter.api.Test;

import static com.deathrayresearch.forrester.measure.Units.DIMENSIONLESS;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class StepTest {

    @Test
    public void shouldReturnZeroBeforeStepTime() {
        int[] step = {0};
        Step formula = Step.of(100, 5, () -> step[0]);
        assertEquals(0, formula.getCurrentValue(), 0.0);
    }

    @Test
    public void shouldReturnHeightAtStepTime() {
        int[] step = {5};
        Step formula = Step.of(100, 5, () -> step[0]);
        assertEquals(100, formula.getCurrentValue(), 0.0);
    }

    @Test
    public void shouldReturnHeightAfterStepTime() {
        int[] step = {10};
        Step formula = Step.of(100, 5, () -> step[0]);
        assertEquals(100, formula.getCurrentValue(), 0.0);
    }

    @Test
    public void shouldSupportNegativeHeight() {
        int[] step = {5};
        Step formula = Step.of(-50, 5, () -> step[0]);
        assertEquals(-50, formula.getCurrentValue(), 0.0);
    }

    @Test
    public void shouldWorkAsFormulaInVariable() {
        int[] step = {0};
        Step formula = Step.of(42, 3, () -> step[0]);
        Variable var = new Variable("Shock", DIMENSIONLESS, formula);

        assertEquals(0, var.getValue(), 0.0);
        step[0] = 3;
        assertEquals(42, var.getValue(), 0.0);
    }
}
